interface Inter {}

class Impl implements Inter {}

class TestGenericsInstanceof<I extends Inter>
{
    I member;

    {
      boolean test = member instanceof Impl;
    }
}
