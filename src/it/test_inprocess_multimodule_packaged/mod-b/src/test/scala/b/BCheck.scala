package b
import a.AFixture
object BCheck { def ok: Boolean = B.go == AFixture.expected + "B" }
