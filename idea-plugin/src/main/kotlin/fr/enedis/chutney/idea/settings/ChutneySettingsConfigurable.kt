/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import fr.enedis.chutney.kotlin.authentication.AuthMethod
import fr.enedis.chutney.kotlin.util.ChutneyServerInfo
import fr.enedis.chutney.kotlin.util.HttpClient
import java.awt.BorderLayout
import java.awt.Color
import java.awt.GridLayout
import javax.swing.*


class ChutneySettingsConfigurable :
  SearchableConfigurable, Configurable.NoScroll {

  val url: JBTextField = JBTextField()

  val user: JBTextField = JBTextField()
  val userLabel: JLabel = JLabel("User: ")
  val password: JBPasswordField = JBPasswordField()
  val passwordLabel: JLabel = JLabel("Password: ")

  val token: JBTextField = JBTextField()
  val tokenLabel: JLabel = JLabel("Token: ")

  val proxyUrl: JBTextField = JBTextField()
  val proxyUser: JBTextField = JBTextField()
  val proxyPassword: JBPasswordField = JBPasswordField()

  val authModeLabel: JLabel = JLabel("Authentication mode")
  val basicAuthButton = JBRadioButton("Basic")
  val bearerAuthButton = JBRadioButton("Bearer")
  val apiKeyAuthButton = JBRadioButton("API-Key")
  val authButtonsGroup = ButtonGroup()

  val checkConnectionButton = JButton("Check connection")

  val chutneySettings: ChutneySettings = ChutneySettings.getInstance()

  override fun isModified(): Boolean {
    return stateFromFields() != chutneySettings.state
  }

  override fun getDisplayName(): String {
    return "Chutney"
  }

  override fun getId(): String {
    return "chutney.tools.settings"
  }

  override fun reset() {
    initFields()
  }

  override fun apply() {
    this.chutneySettings.loadState(stateFromFields())
  }

  private fun basicButtonSelected() {
    updateAuthFields(isBasic = true, isToken = false)
  }

  private fun tokenButtonSelected() {
    updateAuthFields(isBasic = false, isToken = true)
  }

  private fun apiKeyButtonSelected() {
    updateAuthFields(isBasic = false, isToken = true)
  }

  private fun updateAuthFields(isBasic: Boolean, isToken: Boolean) {
    user.isVisible = isBasic
    userLabel.isVisible = isBasic
    password.isVisible = isBasic
    passwordLabel.isVisible = isBasic

    token.isVisible = isToken
    tokenLabel.isVisible = isToken

    checkConnectionButton.isVisible = isBasic || isToken
  }

  private fun stateFromFields() = ChutneySettings.ChutneySettingsState(
    url = url.text,
    auth = if (basicAuthButton.isSelected) AuthMethod.Basic(user.text, String(password.password))
    else if (bearerAuthButton.isSelected) AuthMethod.Bearer(token.text) else AuthMethod.ApiKey(token.text),
    user = user.text,
    password = String(password.password),
    token = token.text,
    proxyUrl = proxyUrl.text,
    proxyUser = proxyUser.text,
    proxyPassword = String(proxyPassword.password)
  )

  private fun initFields() {
    url.text = chutneySettings.state.url
    basicAuthButton.isSelected = chutneySettings.state.auth is AuthMethod.Basic
    bearerAuthButton.isSelected = chutneySettings.state.auth is AuthMethod.Bearer
    apiKeyAuthButton.isSelected = chutneySettings.state.auth is AuthMethod.ApiKey
    user.text =
      if (chutneySettings.state.auth is AuthMethod.Basic) (chutneySettings.state.auth as AuthMethod.Basic).user else ""
    password.text =
      if (chutneySettings.state.auth is AuthMethod.Basic) (chutneySettings.state.auth as AuthMethod.Basic).password else ""
    if (chutneySettings.state.auth is AuthMethod.Bearer) {
      token.text = (chutneySettings.state.auth as AuthMethod.Bearer).token
    } else if (chutneySettings.state.auth is AuthMethod.ApiKey) {
      token.text = (chutneySettings.state.auth as AuthMethod.ApiKey).token
    }
    proxyUrl.text = chutneySettings.state.proxyUrl
    proxyUser.text = chutneySettings.state.proxyUser
    proxyPassword.text = chutneySettings.state.proxyPassword

    updateAuthFields(isBasic = basicAuthButton.isSelected, isToken = bearerAuthButton.isSelected || apiKeyAuthButton.isSelected)
  }

  override fun createComponent(): JComponent {
    initFields()

    val checkLabel = JBLabel("").apply { isVisible = false }

    checkConnectionButton.addActionListener {
      try {
        val serverInfo = ChutneyServerInfo(
          url.text,
          if (basicAuthButton.isSelected) AuthMethod.Basic(user.text, String(password.password))
          else if (bearerAuthButton.isSelected) AuthMethod.Bearer(token.text)
          else AuthMethod.ApiKey(token.text),
          proxyUrl.text.ifBlank { null },
          proxyUser.text.ifBlank { null },
          String(proxyPassword.password).ifBlank { null }
        )
        HttpClient.get<Any>(serverInfo, "/api/v1/user")
        checkLabel.text = "Connection successfull"
        checkLabel.foreground = Color.decode("#297642")
      } catch (exception: Exception) {
        checkLabel.text = "Connection failed: $exception"
        checkLabel.foreground = JBColor.RED
      }
      checkLabel.isVisible = true
    }

    authButtonsGroup.add(basicAuthButton)
    authButtonsGroup.add(bearerAuthButton)
    authButtonsGroup.add(apiKeyAuthButton)

    val authButtonsPanel = JPanel(GridLayout(1, 3))
    authButtonsPanel.add(basicAuthButton)
    authButtonsPanel.add(bearerAuthButton)
    authButtonsPanel.add(apiKeyAuthButton)

    val myWrapper = JPanel(BorderLayout())
    val centerPanel =
      FormBuilder.createFormBuilder()
        .addLabeledComponent("Server url : ", url)
        .addVerticalGap(20)
        .addComponent(authModeLabel)
        .addComponent(authButtonsPanel)
        .addLabeledComponent(userLabel, user)
        .addLabeledComponent(passwordLabel, password)
        .addLabeledComponent(tokenLabel, token)
        .addVerticalGap(20)
        .addLabeledComponent("Proxy url: ", proxyUrl)
        .addLabeledComponent("Proxy user: ", proxyUser)
        .addLabeledComponent("Proxy password: ", proxyPassword)
        .addComponentToRightColumn(checkConnectionButton)
        .addComponent(checkLabel)
        .panel

    basicAuthButton.addActionListener { basicButtonSelected() }
    bearerAuthButton.addActionListener { tokenButtonSelected() }
    apiKeyAuthButton.addActionListener { apiKeyButtonSelected() }

    myWrapper.add(centerPanel, BorderLayout.NORTH)
    return myWrapper
  }

  companion object {
    fun getInstance(): ChutneySettingsConfigurable {
      return ApplicationManager.getApplication().getService(ChutneySettingsConfigurable::class.java)
    }
  }
}
