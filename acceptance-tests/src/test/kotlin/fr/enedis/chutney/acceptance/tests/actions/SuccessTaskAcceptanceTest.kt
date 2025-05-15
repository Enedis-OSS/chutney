/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.acceptance.tests.actions

import fr.enedis.chutney.kotlin.dsl.Scenario
import fr.enedis.chutney.kotlin.dsl.SuccessAction

val `Direct Success` = Scenario(title = "Direct Success") {

  When("Direct success") {
    SuccessAction()
  }
}

val `Substeps Success` = Scenario(title = "Substeps Success") {
  Given("Direct success") {
    SuccessAction()
  }
  When(" I want to have one substep") {
    SuccessAction()
  }
  Then("I want to have more multiple substeps") {
    Step("first substep") {
      SuccessAction()
    }
    Step("second substep") {
      SuccessAction()
    }
  }
}