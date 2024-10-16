package com.rosvit.swimreport

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FormattedDurationSpec extends AnyFlatSpec with Matchers {

  "apply" should "format time in seconds as HH:MM:SS string" in {
    FormattedDuration(5245f) shouldEqual "01:27:25"
  }

  it should "correctly format time less than 1 hour" in {
    FormattedDuration(1996f) shouldEqual "00:33:16"
  }

  "pace" should "format swimming pace as MM:SS string" in {
    FormattedDuration.pace(175f) shouldEqual "02:55"
  }
}
