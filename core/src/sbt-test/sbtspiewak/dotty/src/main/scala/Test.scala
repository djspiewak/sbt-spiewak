object Test {

  def foo[F[_], A](fa: F[A]): F[A] = fa

  foo(Option(42))
  foo(Right(42))    // testing partial unification

  foo[Option, Int](Some(42))
  foo[Option[*], Int](Some(42))    // testing kind-projector things

  foo[Either[String, *], Int](Right(42))
}
