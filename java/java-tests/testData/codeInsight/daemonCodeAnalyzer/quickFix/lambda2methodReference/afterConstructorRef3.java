// "Replace lambda with method reference" "true"
class NonStaticInner3 {
    class Foo {
        Foo(Integer i) {}
        Foo() {}
    }

    interface I1<X> {
        X m(int i);
    }

    interface I2<X> {
        X m();
    }

    interface I3<X> {
        X m(NonStaticInner3 rec, int i);
    }

    interface I4<X> {
        X m(NonStaticInner3 rec);
    }

    {
        I1<Foo> b1 = (i) -> new Foo(i);
        I2<Foo> b2 = () -> new Foo();
    }

    {
        I3<Foo> b1 = Foo::new;
        I4<Foo> b2 = (rec) -> rec.new Foo();
    }
}