/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi.impl.source.resolve.reference;

import com.intellij.patterns.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.filters.ElementFilter;
import com.intellij.psi.filters.position.FilterPattern;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ConcurrentHashMap;
import com.intellij.util.containers.FactoryMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Dmitry Avdeev
 */
public class PsiReferenceRegistrarImpl extends PsiReferenceRegistrar {
  private final ConcurrentMap<Class, SimpleProviderBinding<PsiReferenceProvider>> myBindingsMap = new ConcurrentHashMap<Class, SimpleProviderBinding<PsiReferenceProvider>>();
  private final ConcurrentMap<Class, NamedObjectProviderBinding<PsiReferenceProvider>> myNamedBindingsMap = new ConcurrentHashMap<Class, NamedObjectProviderBinding<PsiReferenceProvider>>();
  private final FactoryMap<Class, Class[]> myKnownSupers = new ConcurrentFactoryMap<Class, Class[]>() {
    @Override
    protected Class[] create(Class key) {
      final Set<Class> result = new LinkedHashSet<Class>();
      for (Class candidate : myBindingsMap.keySet()) {
        if (candidate.isAssignableFrom(key)) {
          result.add(candidate);
        }
      }
      for (Class candidate : myNamedBindingsMap.keySet()) {
        if (candidate.isAssignableFrom(key)) {
          result.add(candidate);
        }
      }
      if (result.isEmpty()) {
        return ArrayUtil.EMPTY_CLASS_ARRAY;
      }
      return result.toArray(new Class[result.size()]);
    }
  };

  /**
   * @deprecated {@see com.intellij.psi.PsiReferenceContributor
   */
  public void registerReferenceProvider(@Nullable ElementFilter elementFilter,
                                        @NotNull Class scope,
                                        @NotNull PsiReferenceProvider provider,
                                        double priority) {
    PsiElementPattern.Capture<PsiElement> capture = PlatformPatterns.psiElement(scope);
    registerReferenceProvider(capture.and(new FilterPattern(elementFilter)), provider, priority);
  }

  @Override
  public <T extends PsiElement> void registerReferenceProvider(@NotNull ElementPattern<T> pattern,
                                                               @NotNull PsiReferenceProvider provider,
                                                               double priority) {
    myKnownSupers.clear(); // we should clear the cache
    final Class scope = pattern.getCondition().getInitialCondition().getAcceptedClass();
    final List<PatternCondition<? super T>> conditions = pattern.getCondition().getConditions();
    for (PatternCondition<? super T> _condition : conditions) {
      if (!(_condition instanceof PsiNamePatternCondition)) {
        continue;
      }
      final PsiNamePatternCondition<?> nameCondition = (PsiNamePatternCondition)_condition;
      List<PatternCondition<? super String>> conditions1 = nameCondition.getNamePattern().getCondition().getConditions();
      for (PatternCondition<? super String> condition1 : conditions1) {
        if (condition1 instanceof ValuePatternCondition) {
          final Collection<String> strings = ((ValuePatternCondition)condition1).getValues();
          registerNamedReferenceProvider(ArrayUtil.toStringArray(strings), nameCondition, scope, true, provider, priority, pattern);
          return;
        }
        if (condition1 instanceof CaseInsensitiveValuePatternCondition) {
          final String[] strings = ((CaseInsensitiveValuePatternCondition)condition1).getValues();
          registerNamedReferenceProvider(strings, nameCondition, scope, false, provider, priority, pattern);
          return;
        }
      }
      break;
    }

    while (true) {
      SimpleProviderBinding<PsiReferenceProvider> providerBinding = myBindingsMap.get(scope);
      if (providerBinding != null) {
        providerBinding.registerProvider(provider, pattern, priority);
        return;
      }

      SimpleProviderBinding<PsiReferenceProvider> binding = new SimpleProviderBinding<PsiReferenceProvider>();
      binding.registerProvider(provider, pattern, priority);
      if (myBindingsMap.putIfAbsent(scope, binding) == null) break;
    }
  }

  /**
   * @deprecated {@link com.intellij.psi.PsiReferenceContributor}
   */
  public void registerReferenceProvider(@Nullable ElementFilter elementFilter,
                                        @NotNull Class scope,
                                        @NotNull PsiReferenceProvider provider) {
    registerReferenceProvider(elementFilter, scope, provider, DEFAULT_PRIORITY);
  }

  public void unregisterReferenceProvider(@NotNull Class scope, @NotNull PsiReferenceProvider provider) {
    ProviderBinding<PsiReferenceProvider> providerBinding = myBindingsMap.get(scope);
    providerBinding.unregisterProvider(provider);
  }


  private void registerNamedReferenceProvider(@NotNull String[] names,
                                              final PsiNamePatternCondition<?> nameCondition,
                                              @NotNull Class scopeClass,
                                              final boolean caseSensitive,
                                              @NotNull PsiReferenceProvider provider,
                                              final double priority,
                                              @NotNull ElementPattern pattern) {
    NamedObjectProviderBinding<PsiReferenceProvider> providerBinding = myNamedBindingsMap.get(scopeClass);

    if (providerBinding == null) {
      providerBinding = ConcurrencyUtil.cacheOrGet(myNamedBindingsMap, scopeClass, new NamedObjectProviderBinding<PsiReferenceProvider>() {
        @Override
        protected String getName(final PsiElement position) {
          return nameCondition.getPropertyValue(position);
        }
      });
    }

    providerBinding.registerProvider(names, pattern, caseSensitive, provider, priority);
  }

  /**
   * @see com.intellij.psi.PsiReferenceContributor
   * @deprecated
   */
  public void registerReferenceProvider(@NotNull Class scope, @NotNull PsiReferenceProvider provider) {
    registerReferenceProvider(null, scope, provider);
  }

  @NotNull
  List<ProviderBinding.ProviderInfo<PsiReferenceProvider,ProcessingContext>> getPairsByElement(@NotNull PsiElement element,
                                                                                               @NotNull PsiReferenceService.Hints hints) {
    final Class<? extends PsiElement> clazz = element.getClass();
    List<ProviderBinding.ProviderInfo<PsiReferenceProvider, ProcessingContext>> ret = null;

    for (Class aClass : myKnownSupers.get(clazz)) {
      SimpleProviderBinding<PsiReferenceProvider> simpleBinding = myBindingsMap.get(aClass);
      NamedObjectProviderBinding<PsiReferenceProvider> namedBinding = myNamedBindingsMap.get(aClass);
      if (simpleBinding == null && namedBinding == null) continue;

      if (ret == null) ret = new SmartList<ProviderBinding.ProviderInfo<PsiReferenceProvider, ProcessingContext>>();
      if (simpleBinding != null) {
        simpleBinding.addAcceptableReferenceProviders(element, ret, hints);
      }
      if (namedBinding != null) {
        namedBinding.addAcceptableReferenceProviders(element, ret, hints);
      }
    }
    return ret == null ? Collections.<ProviderBinding.ProviderInfo<PsiReferenceProvider, ProcessingContext>>emptyList() : ret;
  }
}
