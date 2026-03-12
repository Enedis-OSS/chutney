/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
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
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

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
    val tokenAuthButton = JBRadioButton("Token")
    val authButtonsGroup = ButtonGroup()

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

    private fun updateAuthFields(isBasic: Boolean, isToken: Boolean) {
      user.isVisible = isBasic
      userLabel.isVisible = isBasic
      password.isVisible = isBasic
      passwordLabel.isVisible = isBasic

      token.isVisible = isToken
      tokenLabel.isVisible = isToken
    }

  private fun stateFromFields() = ChutneySettings.ChutneySettingsState(
        url = url.text,
        basicAuth = basicAuthButton.isSelected,
        user = user.text,
        password = String(password.password),
        token = token.text,
        proxyUrl = proxyUrl.text,
        proxyUser = proxyUser.text,
        proxyPassword = String(proxyPassword.password)
    )

    private fun initFields() {
        val serverInfo = chutneySettings.state.serverInfo()
        url.text = serverInfo?.url
        basicAuthButton.isSelected = chutneySettings.state.basicAuth == true
        tokenAuthButton.isSelected = chutneySettings.state.basicAuth == false
        user.text = if(serverInfo?.auth is AuthMethod.Basic) (serverInfo.auth as AuthMethod.Basic).user else ""
        password.text = if(serverInfo?.auth is AuthMethod.Basic) (serverInfo.auth as AuthMethod.Basic).password else ""
        token.text = if(serverInfo?.auth is AuthMethod.Bearer) (serverInfo.auth as AuthMethod.Bearer).token else ""
        proxyUrl.text = serverInfo?.proxyUrl
        proxyUser.text = serverInfo?.proxyUser
        proxyPassword.text = serverInfo?.proxyPassword

        updateAuthFields(isBasic = basicAuthButton.isSelected, isToken = tokenAuthButton.isSelected)
    }

    override fun createComponent(): JComponent {
        initFields()

        val checkConnectionButton = JButton("Check connection")
        val checkLabel = JBLabel("").apply { isVisible = false }

        checkConnectionButton.addActionListener {
            try {
                val serverInfo = ChutneyServerInfo(
                    url.text,
                  if(basicAuthButton.isSelected) AuthMethod.Basic(user.text, String(password.password))
                    else AuthMethod.Bearer(token.text),
                    proxyUrl.text.ifBlank { null },
                    proxyUser.text.ifBlank { null },
                    String(proxyPassword.password).ifBlank { null }
                )
                HttpClient.get<Any>(serverInfo,"/api/v1/user")
                checkLabel.text = "Connection successfull"
                checkLabel.foreground = Color.decode("#297642")
            } catch (exception: Exception) {
                checkLabel.text = "Connection failed: $exception"
                checkLabel.foreground = JBColor.RED
            }
            checkLabel.isVisible = true
        }

        authButtonsGroup.add(basicAuthButton)
        authButtonsGroup.add(tokenAuthButton)

        val myWrapper = JPanel(BorderLayout())
        val centerPanel =
                FormBuilder.createFormBuilder()
                        .addLabeledComponent("Server url : ", url)
                        .addVerticalGap(20)
                        .addComponent(authModeLabel)
                        .addComponent(basicAuthButton)
                        .addComponent(tokenAuthButton)
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
        tokenAuthButton.addActionListener { tokenButtonSelected() }

        myWrapper.add(centerPanel, BorderLayout.NORTH)
        return myWrapper
    }

    companion object {
        fun getInstance(): ChutneySettingsConfigurable {
            return ApplicationManager.getApplication().getService(ChutneySettingsConfigurable::class.java)
        }
    }
}
