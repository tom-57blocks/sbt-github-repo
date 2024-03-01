package xyz.jia.scala.commons.test

import scala.util.Try

import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

trait NthAnswer {

  /** Get the nth return value on each invocation. For example; nthAnswer[Int](1, 2, 3) returns 1 on
    * first invocation, 2 on second invocation, and 3 on third invocation
    *
    * This can be combined with `org.mockito.stubbing.OngoingStubbing#thenAnswer` or
    * `org.mockito.Mockito#doAnswer` if you want the mocked method to return different values on
    * each invocation.
    *
    * @example
    * {{{
    *   when(someMock.doSomeAction(any()))
    *     .thenAnswer(nthAnswer( "answer 1", "answer 2", "answer3" ))
    *
    *   doAnswer(nthAnswer( "answer 1", "answer 2", "answer 3" ))
    *       .when(someMock)
    *       .doSomeAction(any())
    * }}}
    *
    * @param returnValues
    *   value to return (just like what you put inside thenReturn() as varargs)
    * @tparam T
    *   type of the object
    * @return
    *   nth returnValues
    * @throws java.lang.ArrayIndexOutOfBoundsException
    *   when invoked more often than the number of returnValues args
    */
  def nthAnswer[T](returnValues: T*): Answer[T] =
    nthAnswerDeferred(returnValues.map((result: T) => () => result): _*)

  /** Get the nth return value on each invocation. Similar to `NthAnswer#nthAnswer` but it expects
    * the arguments to be deferred in a function so you can easily mock an exception thrown inside
    * the method you are stubbing.
    *
    * @example
    * {{{
    *   when(someMock.doSomeAction(any()))
    *     .thenAnswer(
    *         nthAnswerDeferred( () => "answer 1",
    *         () => throw new IllegalArgumentException("bad things happen"),
    *         () => "answer 3" )
    *     )
    * }}}
    *
    * @param returnValues
    *   function to return
    * @tparam T
    *   type of the object
    * @return
    *   nth returnValues
    * @throws java.lang.ArrayIndexOutOfBoundsException
    *   when invoked more often than the number of returnValues args
    */
  def nthAnswerDeferred[T](returnValues: (() => T)*): Answer[T] = new Answer[T] {

    private var count: Int = 0

    override def answer(invocation: InvocationOnMock): T = {
      count += 1
      val returnValue: T = Try(returnValues(count - 1).apply()).recover {
        case _: ArrayIndexOutOfBoundsException =>
          throw new ArrayIndexOutOfBoundsException(
            s"Trying to invoke nthAnswer for $count times " +
              s"while only providing ${returnValues.toList.length} return values"
          )
      }.get
      returnValue
    }

  }

}
