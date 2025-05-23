<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
  -->
# Contributing

## Summary

* [How to contribute to the documentation](#doc)
* [How to make a Pull Request](#pr)
* [Code convention](#code)
* [Test convention](#test)
* [Branch convention](#branch)
* [Commit changes](#commit)
* [Dependency management](#dep)
* [Build Process](#build)
* [Release Management](#release)
    * [Create keys to sign artifacts](#pgp_create)
    * [Export secret signin key to CI](#pgp_export_secret)
    * [Export public signin key to public servers](#pgp_export_public)
    * [Extend signin key expiration date](#pgp_extend_subkey)
* [Releasing](#releasing)
* [Add a new Task](#task)
* [Licensing](#oss)


## <a name="doc"></a> How to contribute to the documentation

To contribute to this documentation (README, CONTRIBUTING, etc.), we conforms to the [CommonMark Spec](http://spec.commonmark.org/0.27/)

* [https://www.makeareadme.com/#suggestions-for-a-good-readme](https://www.makeareadme.com/#suggestions-for-a-good-readme)
* [https://help.github.com/en/articles/setting-guidelines-for-repository-contributors](https://help.github.com/en/articles/setting-guidelines-for-repository-contributors)


## <a name="pr"></a> How to make a Pull Request

1. Fork the repository and keep active sync on our repo
2. Create your working branches as you like
    * **WARNING** - Do not modify the main branch nor any of our branches since it will break the automatic sync
3. When you are done, fetch all and rebase your branch onto our main or any other of ours
    * ex. on your branch, do :
        * `git fetch --all --prune`
        * `git rebase --no-ff origin/main`
4. Test your changes and make sure everything is working
5. Submit your Pull Request
    * Do not forget to add reviewers ! Check out the last authors of the code you modified and add them.
    * In case of doubts, here are active contributors :


## <a name="code"></a> Code convention

### Naming

Whenever an acronym is included as part of a type name or method name, keep the first
letter of the acronym uppercase and use lowercase for the rest of the acronym. Otherwise,
it becomes _impossible_ to perform camel-cased searches in IDEs, and it becomes
potentially very difficult for mere humans to read or reason about the element without
reading documentation (if documentation even exists).

Consider for example a use case needing to support an HTTP URL. Calling the method
`getHTTPURL()` is absolutely horrible in terms of usability; whereas, `getHttpUrl()` is
great in terms of usability. The same applies for types `HTTPURLProvider` vs
`HttpUrlProvider`, etc.

Whenever an acronym is included as part of a field name or parameter name:

* If the acronym comes at the start of the field or parameter name, use lowercase for the
  entire acronym
    * for example, `String url;`.

* Otherwise, keep the first letter of the acronym uppercase and use lowercase for the
  rest of the acronym
    * for example, `String defaultUrl;`.

### Formatting

We use an [.editorconfig file](http://editorconfig.org/), please use a tool accepting it and do not override rules

#### Imports

It is forbidden to use _wildcard imports_ (e.g., `import static org.assertj.core.api.Assertions.*;`) in Java code.

##### Ordering rules

* import static _all other imports_
* blank line
* import _all other imports_
* blank line

### Javadoc

* Javadoc's comments should be wrapped after 80 characters whenever possible.
* This first paragraph must be a single, concise sentence that ends with a period (".").
* Place `<p>` on the same line as the first line in a new paragraph and precede `<p>` with a blank line.
* Insert a blank line before at-clauses/tags.
* Favor `{@code foo}` over `<code>foo</code>`.
* Favor literals (e.g., `{@literal @}`) over HTML entities.
* Use `@since 5.0` instead of `@since 5.0.0`.

### Packages

Adding a new task capabilities should be packaged in a maven submodule named chutney-task-\[task-name\]

## <a name="test"></a> Test convention

### Naming

* All test classes must end with a `Test` suffix.
* Example test classes that should not be picked up by the build must end with a `TestCase` suffix.

### Assertions

* Use `org.assertj.core.api.Assertions` wherever possible.
* Use `org.junit.Assert` if sufficient.

### Mocking

* Use either Mockito or hand-written test doubles.
* Use `org.springframework.test.web.servlet.MockMvc` to mock REST HTTP endpoints
* **Do not use PowerMock**
    * We consider it to be sign of a code-smell

## <a name="branch"></a> Branch convention

* **wip/** unstable code, to share between developers working on the same task
* **feat/** stable code of new feature, to be merged if validated
* **fix/** stable code of correction (PROD / VALID)
* **tech/** stable code, purely technical modification like refactoring, log level change or documentation


## <a name="commit"></a> Commit changes

### Signature

While not mandatory, we would like to have signed commits as much as possible.

* Install GPG according to your OS and distribution
* Create a GPG key following : https://docs.github.com/en/github/authenticating-to-github/generating-a-new-gpg-key
* Add the GPG key to your Github account : https://docs.github.com/en/github/authenticating-to-github/adding-a-new-gpg-key-to-your-github-account
* Configure git to use and sign your commit using your gpg key :
    * ```git config --global user.signingkey KEY_ID```
    * ```git config --global commit.gpgsign true```
* Configure a gpg agent to avoid typing your passphrase for every commit, for example on linux, you can edit ```~/.gnupg/gpg-agent.conf``` :
  ```
  default-cache-ttl 43200
  max-cache-ttl 43200
  ```

### Commit message
As a general rule, the style and formatting of commit messages should follow the guidelines in
[How to Write a Git Commit Message](http://chris.beams.io/posts/git-commit/).

* Separate subject from body with a blank line
* Limit the subject line to 50 characters
* Capitalize the subject line
* Do not end the subject line with a period
* Use the imperative mode in the subject line
* Wrap the body at 72 characters
* Use the body to explain what and why vs. how


**Alternative**:

* http://karma-runner.github.io/0.10/dev/git-commit-msg.html


## <a name="build"></a> Build Process

We use Github Actions to build and release Chutney.
[![Build](https://github.com/Enedis-OSS/chutney/workflows/Build/badge.svg?branch=main)](https://github.com/Enedis-OSS/chutney/actions)

## <a name="release"></a> Release Management

### <a name="pgp_create"></a> Signing release artifacts (Maintener)

We need a key to sign our artifacts.

In order to do so :
* We will create an organization 'master key', having one password and without expiration date.
    * It is used only to create and manage subkeys
    * It should never be shared outside the core team
    * It should be kept preciously from being lost or stolen

* We will create a 'signing subkey', with another password and with an expiration date.
    * The 'subkey' will be used daily.
    * If the 'subkey' is compromised, the 'master key' should still be safe

* List secret keys
  > gpg -K --with-keygrip --keyid-format LONG

* List public keys
  > gpg -k --with-keygrip --keyid-format LONG


1. Create a master key : ```gpg --full-generate-key```
2. Check created keys :  ```gpg -K --with-keygrip --keyid-format LONG```
   ```
   sec   rsa4096/BA9185485BBC958B 2022-05-04 [SC]
   809E390126D4C2139ADDAD97BA9185485BBC958B
   Keygrip = BA28B56AFCAC6FD8C5249BBF3A8A7FF06E3831C5
   uid                [  ultime ] Bruce Wayne <bruce.wayne@comics.dc>
   ssb   rsa4096/84CF518A69BBA856 2022-05-04 [E]
   Keygrip = 7E3B41B0931CE0AE8A8D331E62D51EE63329D202
   ```

3. Save the master key: ```gpg --export-secret-keys KEY_ID > you_master_key_file.pgp```
   ```
   gpg --export-secret-keys 809E390126D4C2139ADDAD97BA9185485BBC958B > batman_master_secret_key.pgp
   ```

4. Add a signin subkey valid 1 year : ```gpg --quick-addkey KEY_ID rsa4096 sign 1y```
   ```
   gpg --quick-addkey 809E390126D4C2139ADDAD97BA9185485BBC958B rsa4096 sign 1y
   ```

5. Check keys : ```gpg --list-keys --with-subkey-fingerprints```
   ```
   pub   rsa4096 2022-05-04 [SC]
   809E390126D4C2139ADDAD97BA9185485BBC958B
   uid          [  ultime ] Bruce Wayne <bruce.wayne@comics.dc>
   sub   rsa4096 2022-05-04 [E]
   8C4B21B825F81FA36BA6EF0684CF518A69BBA856
   sub   rsa4096 2022-05-04 [S] [expire: 2023-05-04]
   DB7568F90E8783C0A9B84BDCB268801F1D3F9DC7
   ```
6. Delete the master key : ```gpg --delete-secret-keys KEY_ID```
    * **Be carefull not to delete the subkey**

   ```
   gpg --delete-secret-keys 809E390126D4C2139ADDAD97BA9185485BBC958B
   ```

7. Verify that only the master key is deleted : ```gpg -K```
    * You should see a # following the secret key, indicating it is no longer available.
   ```
   sec#  rsa4096/BA9185485BBC958B 2022-05-04 [SC]
   809E390126D4C2139ADDAD97BA9185485BBC958B
   Keygrip = BA28B56AFCAC6FD8C5249BBF3A8A7FF06E3831C5
   uid                [  ultime ] Bruce Wayne <bruce.wayne@comics.dc>
   ssb   rsa4096/84CF518A69BBA856 2022-05-04 [E]
   Keygrip = 7E3B41B0931CE0AE8A8D331E62D51EE63329D202
   ssb   rsa4096/B268801F1D3F9DC7 2022-05-04 [S] [expire: 2023-05-04]
   Keygrip = C3AED3B6A69C42F0F3BCD78E8F1408C8AA48F43
   ```

8. Change the passphrase for the subkey : ```gpg --edit-key KEY_ID```
   ```
   gpg --edit-key 809E390126D4C2139ADDAD97BA9185485BBC958B
   ```
    * On the prompt:
        * ```passwd```
            * (enter original passphrase. ie. the one from the master key)
            * (enter twice the new passphrase you want for the subkey)
        * ```save```

9. You are done creating the keys :)
10. Now safely backup the masterkey pgp file created on step 3 and safely store its passphrase !
11. Set the secret signin subkey on CI, follow [instruction to export secret signin subkey](#pgp_export_secret)
12. Publish the public subkey on servers, follow [instruction to export public signin subkey](#pgp_export_public)

#### <a name="pgp_export_secret"></a> Export secret signin subkey

In order to sign artifacts during CI builds, we should provide the secret signin subkey

1. List keys : ```gpg -K --with-keygrip --keyid-format LONG```
    ```
      sec   rsa4096/BA9185485BBC958B 2022-05-04 [SC]
      809E390126D4C2139ADDAD97BA9185485BBC958B
      Keygrip = BA28B56AFCAC6FD8C5249BBF3A8A7FF06E3831C5
      uid                [  ultime ] Bruce Wayne <bruce.wayne@comics.dc>
      ssb   rsa4096/84CF518A69BBA856 2022-05-04 [E]
      Keygrip = 7E3B41B0931CE0AE8A8D331E62D51EE63329D202
      ssb   rsa4096/B268801F1D3F9DC7 2022-05-04 [S] [expire: 2022-05-05]
      Keygrip = C3AED3B6A69C42F0F3BCD78E8F1408C8AA48F43C
    ```

2. Export to ascii armor format : ```gpg -a --export-secret-subkeys B268801F1D3F9DC7 > bruce_secret_signin_subkey.asc```
    * Enter subkey passphrase
    * Then enter the masterkey passphrase
3. Create organisation or project secret (ex. CHUTNEY_GPG_PRIVATE_KEY) and copy/paste ascii armor key content :
    ```
    -----BEGIN PGP PRIVATE KEY BLOCK-----
    [...]
    -----END PGP PRIVATE KEY BLOCK-----
    ```

#### <a name="pgp_export_public"></a> Export public signin subkey

In order to let Sonatype OSSRH and others check our artifacts signature, we should publish our public signin key.

1. ```gpg -a --export B041166A290ECDF7 > chutney_signin_public_key.pub```
2. Go to a public server like [keyserver.ubuntu.com](https://keyserver.ubuntu.com) or [keys.openpgp.org](keys.openpgp.org) and copy/paste ascii armor key content :
    ```
    -----BEGIN PGP PUBLIC KEY BLOCK-----
    [...]
    -----END PGP PUBLIC KEY BLOCK-----
    ```

#### <a name="pgp_extend_subkey"></a> Extend the subkey expiration date

1. Import the master key : ```gpg --import batman_master_secret_key.pgp```
    * Listing keys with ```gpg -Kv --keyid-format LONG``` should show both a 'sec' '[SC]' master key and a subkey 'ssb' signing key '[S]' with an expiration date.
2. Edit the key using the master key id: ```gpg --edit-key 809E390126D4C2139ADDAD97BA9185485BBC958B```
3. In prompt:
    * Select the subkey : ```key i``` (where i is the index of the listed keys)
        * You will see an ```*``` on the selected key
    * Change expiration : ```expire```
    * Enter the master key passphrase
    * save : ```save```
4. Update the public subkey on servers, follow [instruction](#pgp_export_public)
5. Update the secret subkey on CI follow [instruction](#pgp_export_secret)

## <a name="releasing"></a> Releasing

Chutney releases fall into two categories: those published on Maven Central and those available only on GitHub.

### Update Changelog file

Always update the changelog before creating a release. This ensures the changes are documented for the release being created.
Refer to this page for guidance on automatically generated release notes:  [Automatically generated release notes](https://docs.github.com/en/repositories/releasing-projects-on-github/automatically-generated-release-notes)

Do not hesitate to update the release note generated especially the titles of pull request :)
Use it to update [CHANGELOG.md](https://github.com/Enedis-OSS/chutney/blob/main/CHANGELOG.md)

### Releasing version to Maven Central

#### General information
- Releases on Maven Central follow the format X.X.X (e.g., `2.7.2` or `2.9.4`).
- The subsequent snapshot version will be X.X.X-SNAPSHOT (e.g., `2.7.3-SNAPSHOT` or `2.9.5-SNAPSHOT`).
- The `CHANGELOG.md` file is committed to the release branch, including all previous releases (not including those on Maven Central) since the last Maven Central release.
- Pull requests are mandatory for releases going to Maven Central.

#### Step by step for maven release

```shell
git switch -c release/<RELEASE_VERSION>
git add CHANGELOG.md (see Update Changelog file section)
Update manually `chutneyVersion`  in [idea-plugin/gradle.properties](https://github.com/Enedis-OSS/chutney/blob/main/idea-plugin/gradle.properties)
mvn versions:set -DnewVersion=<RELEASE_VERSION> -DgenerateBackupPoms=false
mvn versions:set-scm-tag -DnewTag=<RELEASE_VERSION> -DgenerateBackupPoms=false
git add .
git diff --staged
git commit -m "chore: Release <RELEASE_VERSION>"
git push origin
```

Now you can open a pull request.  
After validation and merge, you can now tag and push to trigger the release job :
- Update you local main branch.
```shell
git fetch all --prune && git checkout main && git merge --ff-only origin/main
```

- Then tag and push.
```shell
git tag <TAG_VERSION>
git push origin <TAG_VERSION>
```

#### Update Github release note

- Update the release note on [github](https://github.com/Enedis-OSS/chutney/releases)

#### Release on Maven central

In order to release artifacts on maven central, you should first create an account on [sonatype](https://issues.sonatype.org/).  
Then, a team member should open a ticket to ask for granting you release permissions (ex. [ticket](https://issues.sonatype.org/browse/OSSRH-78321))

Then, you can go to [nexus](https://s01.oss.sonatype.org/) and login using your sonatype credentials.

In order to effectively release artifacts :

- Under left menu "Build Promotion" -> "Staging Repositories"
- Select the repository and click on "Close"
- After the checks are done, refresh and click on "Release"
- Wait a few hours to see it on [central](https://central.sonatype.dev/namespace/fr.enedis.chutney)

#### Prepare next development

```shell
Update manually `chutneyVersion`  in [idea-plugin/gradle.properties](https://github.com/Enedis-OSS/chutney/blob/main/idea-plugin/gradle.properties)
mvn versions:set -DnewVersion=<NEXT_DEV_VERSION> -DgenerateBackupPoms=false
mvn versions:set-scm-tag -DnewTag=HEAD -DgenerateBackupPoms=false
git diff HEAD
git add . && git commit -m "chore: Prepare next development <NEXT_DEV_VERSION>"
git push origin
```

### Releasing version only on GitHub

#### General information

- Releases to GitHub only update the last digit of the version (e.g., `2.7.1.1` or `2.9.4.2`).
- The subsequent snapshot version remains unchanged.
- The `CHANGELOG.md` file is committed to the tag.
- Pull requests are not required for releases to GitHub.

#### Step by step for maven release

```shell
git switch -c release/<RELEASE_VERSION>
git add CHANGELOG.md (see Update Changelog file section)
Update manually `chutneyVersion`  in [idea-plugin/gradle.properties](https://github.com/Enedis-OSS/chutney/blob/main/idea-plugin/gradle.properties)
mvn versions:set -DnewVersion=<RELEASE_VERSION> -DgenerateBackupPoms=false
mvn versions:set-scm-tag -DnewTag=<RELEASE_VERSION> -DgenerateBackupPoms=false
git add .
git diff --staged
git commit -m "chore: Release <RELEASE_VERSION>"
```

- Then tag and push.
```shell
git tag <TAG_VERSION>
git push origin <TAG_VERSION>
```

- Update the release note on [github](https://github.com/Enedis-OSS/chutney/releases)

## <a name="task"></a> Adding a task

Create a new maven module with _chutney-parent_ as parent.
And name your module such as _chutney-task-\[task-name\]_
  ```xml
  <parent>
      <groupId>fr.enedis.chutney</groupId>
      <artifactId>chutney-parent</artifactId>
  </parent>
  ```

* Add a dependency on _chutney-action-spi_
    * Transitive dependencies are :
        * com.google.guava

  ```xml
  <dependency>
      <groupId>fr.enedis.chutney</groupId>
      <artifactId>action-spi</artifactId>
  </dependency>
  ```


* Create a class which implements `fr.enedis.chutney.action.spi.Task` interface
* Name your task in CamelCase. It will be converted in spinal-case such as `camel-case` for use in
  when writing scenarios and to tell the engine which task to pick for execution

* Create a constructor with your task parameters annotated with `fr.enedis.chutney.action.spi.injectable.Input`
    * You can also use `fr.enedis.chutney.action.spi.injectable.Target` and `fr.enedis.chutney.action.spi.injectable.Logger`

* Override the `execute()` method
    * This is where your task logic starts

* Feel free to decoupled your code and add any other classes, entities and services up to your needs

* Add a file `META-INF/extension/chutney.tasks`
    * Add the canonical class name of your Task implementation, ex. `fr.enedis.chutney.action.implementation.http.HttpClientTask`
    * If you have multiple `Task` implementations, add them one by line

* When you are done, in order to use your newly created task, add it as a dependency to your _packaging_ module and build it

## <a name="oss"></a> Licensing

We choose to apply the Apache License 2.0 (ALv2) : [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

As for any project, license compatibility issues may arise and should be taken care of.

Concrete instructions and tooling to keep Chutney ALv2 compliant and limit licensing issues are to be found below.

However, we acknowledge topic's complexity, mistakes might be done and we might not get it 100% right.

Still, we strive to be compliant and be fair, meaning, we do our best in good faith.

As such, we welcome any advice and change request.


To any contributor, we strongly recommend further reading and personal research :
* [http://www.apache.org/licenses/](http://www.apache.org/licenses/)
* [http://www.apache.org/legal/](http://www.apache.org/legal/)
* [http://apache.org/legal/resolved.html](http://apache.org/legal/resolved.html)
* [http://www.apache.org/dev/apply-license.html](http://www.apache.org/dev/apply-license.html)
* [http://www.apache.org/legal/src-headers.html](http://apache.org/legal/src-headers.html)
* [http://www.apache.org/legal/release-policy.html](http://www.apache.org/legal/release-policy.html)
* [http://www.apache.org/dev/licensing-howto.html](http://www.apache.org/dev/licensing-howto.html)

* [Why is LGPL not allowed](https://issues.apache.org/jira/browse/LEGAL-192)
* https://issues.apache.org/jira/projects/LEGAL/issues/

* General news : [https://opensource.com/tags/law](https://opensource.com/tags/law)

### How to manage license compatibility

When adding a new dependency, **one should check its license and all its transitive dependencies** licenses.

ALv2 license compatibility as defined by the ASF can be found here : [http://apache.org/legal/resolved.html](http://apache.org/legal/resolved.html)

3 categories are defined :
* [Category A](https://www.apache.org/legal/resolved.html#category-a) : Contains all compatibles licenses.
* [Category B](https://www.apache.org/legal/resolved.html#category-b) : Contains compatibles licenses under certain conditions.
* [Category X](https://www.apache.org/legal/resolved.html#category-x) : Contains all incompatibles licenses which must be avoid at all cost.

__As far as we understand :__

If, by any mean, your contribution should rely on a Category X dependency, then you must provide a way to modularize it
and make it's use optional to Chutney, as a plugin.

You may distribute your plugin under the terms of the Category X license.

Any distribution of Chutney bundled with your plugin will probably be done under the terms of the Category X license.

But _"you can provide the user with instructions on how to obtain and install the non-included"_ plugin.

__References :__
- [Optional](https://www.apache.org/legal/resolved.html#optional)
- [Prohibited](https://www.apache.org/legal/resolved.html#prohibited)

### How to comply with Redistribution and Attribution clauses

Lots of licenses place conditions on redistribution and attribution, including ALv2.

__References :__
* http://mail-archives.apache.org/mod_mbox/www-legal-discuss/201502.mbox/%3CCAAS6%3D7gzsAYZMT5mar_nfy9egXB1t3HendDQRMUpkA6dqvhr7w%40mail.gmail.com%3E
* http://mail-archives.apache.org/mod_mbox/www-legal-discuss/201501.mbox/%3CCAAS6%3D7jJoJMkzMRpSdJ6kAVSZCvSfC5aRD0eMyGzP_rzWyE73Q%40mail.gmail.com%3E

#### LICENSE file
##### In Source distribution

This file contains :
* the complete ALv2 license.
* list dependencies and points to their respective license file
    * Example :
      _This product bundles SuperWidget 1.2.3, which is available under a
      "3-clause BSD" license.  For deails, see deps/superwidget/_
* do not list dependencies under the ALv2

#### NOTICE file

##### In source distribution

_The NOTICE file is not for conveying information to downstream consumers -- it
is a way to *compel* downstream consumers to *relay* certain required notices._

### Unresolved questions - HELP WANTED -
* Should test dependencies be taken into account for source distribution ?
    * It appears to be YES
* Should build time dependencies be taken into account ?
    * It appears to be NO but might depend on the actual stuff done by this dependency
