<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Copyright (C) 2014 OpenThings Project
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" exclude-result-prefixes="xs"
                xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl" version="2.0"
                xmlns:f="http://www.essepuntato.it/xslt/function">

    <xsl:function name="f:string-last-index-of" as="xs:integer*">
        <xsl:param name="source" as="xs:string" />
        <xsl:param name="pattern" as="xs:string" />

        <xsl:variable name="result" select="f:string-index-of($source,$pattern)" as="xs:integer*" />
        <xsl:sequence select="$result[count($result)]" />
    </xsl:function>

    <xsl:function name="f:string-first-index-of" as="xs:integer*">
        <xsl:param name="source" as="xs:string" />
        <xsl:param name="pattern" as="xs:string" />

        <xsl:variable name="result" select="f:string-index-of($source,$pattern)" as="xs:integer*" />
        <xsl:sequence select="$result[1]" />
    </xsl:function>

    <xsl:function name="f:string-index-of" as="xs:integer*">
        <xsl:param name="source" as="xs:string" />
        <xsl:param name="pattern" as="xs:string" />

        <xsl:variable name="tokens" select="tokenize($source,$pattern)" as="xs:string+" />
        <xsl:sequence select="for $i in (1 to count($tokens) - 1) return sum(for $token in $tokens[position() &gt;= 1 and position() &lt;= $i] return string-length($token)) + $i" />
    </xsl:function>



    <!-- TEST -->
    <!-- TEST f:string-first-index-of (BEGIN)  -->
    <xsl:template match="xsl:function[@name = 'f:string-first-index-of']">
        <xsl:variable name="p1" select="'foo/faa/foo'" />
        <xsl:variable name="p2" select="'/'" />
        <function>
            <xsl:call-template name="get.signature" />

            <xsl:variable name="p1" select="'foo/faa'" />
            <xsl:variable name="p2" select="'/'" />
            <xsl:call-template name="get.test">
                <xsl:with-param name="params" select="($p1,$p2)" />
                <xsl:with-param name="result" select="f:string-first-index-of($p1,$p2)" />
            </xsl:call-template>

            <xsl:variable name="p1" select="'foo/faa/foo'" />
            <xsl:variable name="p2" select="'/'" />
            <xsl:call-template name="get.test">
                <xsl:with-param name="params" select="($p1,$p2)" />
                <xsl:with-param name="result" select="f:string-first-index-of($p1,$p2)" />
            </xsl:call-template>

            <xsl:variable name="p1" select="'foo/faa/foo'" />
            <xsl:variable name="p2" select="'#'" />
            <xsl:call-template name="get.test">
                <xsl:with-param name="params" select="($p1,$p2)" />
                <xsl:with-param name="result" select="f:string-first-index-of($p1,$p2)" />
            </xsl:call-template>
        </function>
    </xsl:template>
    <!-- TEST (END) f:string-first-index-of  -->

    <!-- TEST f:string-last-index-of (BEGIN)  -->
    <xsl:template match="xsl:function[@name = 'f:string-last-index-of']">
        <xsl:variable name="p1" select="'foo/faa/foo'" />
        <xsl:variable name="p2" select="'/'" />
        <function>
            <xsl:call-template name="get.signature" />

            <xsl:variable name="p1" select="'foo/faa'" />
            <xsl:variable name="p2" select="'/'" />
            <xsl:call-template name="get.test">
                <xsl:with-param name="params" select="($p1,$p2)" />
                <xsl:with-param name="result" select="f:string-last-index-of($p1,$p2)" />
            </xsl:call-template>

            <xsl:variable name="p1" select="'foo/faa/foo'" />
            <xsl:variable name="p2" select="'/'" />
            <xsl:call-template name="get.test">
                <xsl:with-param name="params" select="($p1,$p2)" />
                <xsl:with-param name="result" select="f:string-last-index-of($p1,$p2)" />
            </xsl:call-template>

            <xsl:variable name="p1" select="'foo/faa/foo'" />
            <xsl:variable name="p2" select="'#'" />
            <xsl:call-template name="get.test">
                <xsl:with-param name="params" select="($p1,$p2)" />
                <xsl:with-param name="result" select="f:string-last-index-of($p1,$p2)" />
            </xsl:call-template>
        </function>
    </xsl:template>
    <!-- TEST (END) f:string-last-index-of  -->

    <!-- TEST f:string-index-of (BEGIN) -->
    <xsl:template match="xsl:function[@name = 'f:string-index-of']">
        <xsl:variable name="p1" select="'foo/faa/foo'" />
        <xsl:variable name="p2" select="'/'" />
        <function>
            <xsl:call-template name="get.signature" />

            <xsl:variable name="p1" select="'foo/faa'" />
            <xsl:variable name="p2" select="'/'" />
            <xsl:call-template name="get.test">
                <xsl:with-param name="params" select="($p1,$p2)" />
                <xsl:with-param name="result" select="f:string-index-of($p1,$p2)" />
            </xsl:call-template>

            <xsl:variable name="p1" select="'foo/faa/foo'" />
            <xsl:variable name="p2" select="'/'" />
            <xsl:call-template name="get.test">
                <xsl:with-param name="params" select="($p1,$p2)" />
                <xsl:with-param name="result" select="f:string-index-of($p1,$p2)" />
            </xsl:call-template>

            <xsl:variable name="p1" select="'foo/faa/foo'" />
            <xsl:variable name="p2" select="'#'" />
            <xsl:call-template name="get.test">
                <xsl:with-param name="params" select="($p1,$p2)" />
                <xsl:with-param name="result" select="f:string-index-of($p1,$p2)" />
            </xsl:call-template>
        </function>
    </xsl:template>
    <!-- TEST (END) f:string-index-of -->
    <!-- TEST f:string-last-index-of (END)  -->


    <!--
    <xsl:template match="text()[ancestor::element()[namespace-uri() = 'http://www.w3.org/1999/XSL/Transform']]" />

    <xsl:template match="/">
        <tests>
            <xsl:apply-templates />
        </tests>
    </xsl:template>
    -->
    <xsl:template name="get.signature">
        <xsl:attribute name="name">
            <xsl:value-of select="@name" />
        </xsl:attribute>
        <input>
            <xsl:for-each select="xsl:param">
                <param>
                    <name><xsl:value-of select="@name" /></name>
                    <type><xsl:value-of select="if (@as) then @as else 'NONE'" /></type>
                </param>
            </xsl:for-each>
        </input>
        <returnType>
            <xsl:value-of select="if (@as) then @as else 'NONE'" />
        </returnType>
    </xsl:template>

    <xsl:template name="get.test">
        <xsl:param name="params" as="item()*" />
        <xsl:param name="result" />
        <test>
            <params>
                <xsl:for-each select="xsl:param">
                    <xsl:variable name="pos" select="position()" as="xs:integer" />
                    <xsl:element name="{@name}">
                        <xsl:value-of select="$params[$pos]" />
                    </xsl:element>
                </xsl:for-each>
            </params>
            <result>
                <xsl:value-of select="$result" separator=" , " />
            </result>
        </test>
    </xsl:template>
</xsl:stylesheet>
