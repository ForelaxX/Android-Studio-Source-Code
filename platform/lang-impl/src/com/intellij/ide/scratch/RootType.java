/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.ide.scratch;

import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.lang.Language;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.LanguageSubstitutors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;

/**
 * @author gregsh
 *
 * Created on 1/19/15
 */
public abstract class RootType {

  public static final ExtensionPointName<RootType> ROOT_EP = ExtensionPointName.create("com.intellij.scratch.rootType");

  @NotNull
  public static RootType[] getAllRootIds() {
    return Extensions.getExtensions(ROOT_EP);
  }

  @NotNull
  public static RootType findById(@NotNull String id) {
    for (RootType type : getAllRootIds()) {
      if (id.equals(type.getId())) return type;
    }
    throw new AssertionError(id);
  }

  @NotNull
  public static <T extends RootType> T findByClass(Class<T> aClass) {
    return Extensions.findExtension(ROOT_EP, aClass);
  }

  private final String myId;
  private final String myDisplayName;

  protected RootType(@NotNull String id, @Nullable String displayName) {
    myId = id;
    myDisplayName = displayName;
  }

  @NotNull
  public final String getId() {
    return myId;
  }

  @Nullable
  public final String getDisplayName() {
    return myDisplayName;
  }

  public boolean isHidden() {
    return StringUtil.isEmpty(myDisplayName);
  }

  @Nullable
  public Language substituteLanguage(@NotNull Project project, @NotNull VirtualFile file) {
    return substituteLanguageImpl(getOriginalLanguage(file), file, project);
  }

  @Nullable
  public Icon substituteIcon(@NotNull Project project, @NotNull VirtualFile file) {
    Language language = substituteLanguage(project, file);
    LanguageFileType fileType = language != null ? language.getAssociatedFileType() : null;
    return fileType != null ? fileType.getIcon() : null;
  }

  @Nullable
  public String substituteName(@NotNull Project project, @NotNull VirtualFile file) {
    return null;
  }

  public VirtualFile findFile(@Nullable Project project, @NotNull String pathName, ScratchFileService.Option option) throws IOException {
    return ScratchFileService.getInstance().findFile(this, pathName, option);
  }

  public void fileOpened(@NotNull VirtualFile file, @NotNull FileEditorManager source) {
  }

  @Nullable
  protected static Language substituteLanguageImpl(Language language, VirtualFile file, Project project) {
    return language != null && language != ScratchFileType.INSTANCE.getLanguage() ?
           LanguageSubstitutors.INSTANCE.substituteLanguage(language, file, project) : language;
  }

  @Nullable
  protected static FileType getOriginalFileType(@NotNull VirtualFile file) {
    String extension = file.getExtension();
    if (extension == null) return null;
    return FileTypeManager.getInstance().getFileTypeByExtension(extension);
  }

  @Nullable
  protected static Language getOriginalLanguage(@NotNull VirtualFile file) {
    FileType fileType = getOriginalFileType(file);
    return fileType instanceof LanguageFileType ? ((LanguageFileType)fileType).getLanguage() : null;
  }

  public boolean isIgnored(@NotNull Project project, @NotNull VirtualFile element) {
    return false;
  }

  public void registerTreeUpdater(@NotNull Project project, @NotNull AbstractTreeBuilder builder) {
  }

}
