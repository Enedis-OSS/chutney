<!--
  ~ SPDX-FileCopyrightText: 2017-2024 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
-->

!!! info "[Browse implementation](https://github.com/Enedis-OSS/chutney/blob/main/chutney/action-impl/src/main/java/com/chutneytesting/action/function/EscapeFunctions.java){:target="_blank"}"

Following functions help you
 * Escape and unescape strings from JSON, HTML, XML and SQL (Usage of [StringEscapeUtils](https://commons.apache.org/proper/commons-text/javadocs/api-release/org/apache/commons/text/StringEscapeUtils.html){:target="_blank"}). 
 * Encode and decode strings for URL (Usage of [URLEncoder](https://devdocs.io/openjdk~21/java.base/java/net/urlencoder){:target="_blank"} and [URLDecoder](https://devdocs.io/openjdk~21/java.base/java/net/urldecoder){:target="_blank"}).

# JSON

!!! note "String escapeJson(String text)"

    **Examples** :

    SpEL : `${#escapeJson("text")}`

!!! note "String unescapeJson(String text)"

    **Examples** :

    SpEL : `${#unescapeJson("text")}`

# HTML

!!! note "String escapeHtml3(String text)"

    **Examples** :

    SpEL : `${#escapeHtml13("text")}`

!!! note "String escapeHtml4(String text)"

    **Examples** :

    SpEL : `${#escapeHtml14("text")}`

!!! note "String unescapeHtml3(String text)"

    **Examples** :

    SpEL : `${#unescapeHtml3("text")}`

!!! note "String unescapeHtml4(String text)"

    **Examples** :

    SpEL : `${#unescapeHtml4("text")}`

# SQL

!!! note "String escapeSql(String sql)"

    **Examples** :

    SpEL : `${#escapeSql("sql")}`

# XML

!!! note "String unescapeXml(String text)"

    **Examples** :

    SpEL : `${#unescapeXml("text")}`

!!! note "String escapeXml10(String text)"

    **Examples** :

    SpEL : `${#escapeXml10("text")}`

!!! note "String escapeXml11(String text)"

    **Examples** :

    SpEL : `${#escapeXml11("text")}`

# URL

!!! note "String urlEncode(String toEncode, String charset)"

    If `charset` is null, use the default charset.
    
    **Examples** :

    SpEL : `${#urlEncode("text", null)}`

!!! note "String urlDecode(String toDecode, String charset)"

    If `charset` is null, use the default charset.
    
    **Examples** :

    SpEL : `${#urlDecode("text", null)}`
