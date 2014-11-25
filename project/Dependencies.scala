import sbt._

object Dependencies {
  object versions {
    val apacheCurator     = "2.4.2"
    val balboa            = "0.14.0"
    val c3po              = "0.9.2.1"
    val codahaleMetrics   = "3.0.2"
    val dropWizardMetrics = "3.1.0"
    val javaxServletApi   = "2.5"
    val liquibaseCore     = "2.0.0"
    val liquibasePlugin   = "1.9.5.0"
    val log4j             = "1.2.16"
    val metricsScala      = "3.3.0"
    val mortbayJetty      = "6.1.22"
    val postgresql        = "9.1-901-1.jdbc4"
    val quasiQuotes       = "2.0.0"
    val rojomaJson        = "2.4.3"
    val rojomaSimpleArm   = "1.2.0"
    val rojomaSimpleArmV2 = "2.0.0"
    val scalaj            = "0.3.15"
    val socrataHttp       = "2.3.4"
    val soqlBrita         = "1.3.0"
    val soqlReference     = "0.3.2"
    val thirdPartyUtils   = "2.6.2"
    val typesafeConfig    = "1.0.2"

    // Test
    val mockito           = "1.10.7"
    val mockServer        = "3.0"
    val scalaCheck        = "1.10.0"
    val scalaMock         = "3.1.RC1"
    val scalaTest         = "2.2.0"
    val springTest        = "3.2.10.RELEASE"
    val wiremock          = "1.46"
  }

  val apacheCuratorDiscovery   = "org.apache.curator" % "curator-x-discovery"   % versions.apacheCurator

  val balboaClient             = "com.socrata"     %% "balboa-client"               % versions.balboa
  val balboaCommon             = "com.socrata"     %% "balboa-common"               % versions.balboa

  val c3po                     = "com.mchange"      % "c3p0"                        % versions.c3po

  val codahaleMetricsGraphite   = "com.codahale.metrics"  % "metrics-graphite"      % versions.codahaleMetrics

  val dropWizardMetricsGraphite = "io.dropwizard.metrics" % "metrics-graphite"      % versions.dropWizardMetrics
  val dropWizardMetricsJetty    = "io.dropwizard.metrics" % "metrics-jetty9"        % versions.dropWizardMetrics

  val javaxServletApi          = "javax.servlet"    % "servlet-api"                 % versions.javaxServletApi % "provided"

  val liquibaseCore            = "org.liquibase"    % "liquibase-core"              % versions.liquibaseCore

  val liquibasePlugin          = "org.liquibase"    % "liquibase-plugin"            % versions.liquibasePlugin

  val log4j                    = "log4j"            % "log4j"                       % versions.log4j

  val metricsScala             = "nl.grons"        %% "metrics-scala"               % versions.metricsScala

  val mortbayJetty             = "org.mortbay.jetty" % "jetty"                      % versions.mortbayJetty % "container"

  val postgresql               = "postgresql"       % "postgresql"                  % versions.postgresql

  val quasiQuotes              = "org.scalamacros" %% "quasiquotes"                 % versions.quasiQuotes

  val rojomaJson               = "com.rojoma"      %% "rojoma-json"                 % versions.rojomaJson

  val rojomaSimpleArm          = "com.rojoma"      %% "simple-arm"                  % versions.rojomaSimpleArm
  val rojomaSimpleArmV2        = "com.rojoma"      %% "simple-arm-v2"               % versions.rojomaSimpleArmV2

  val scalajHttp               = "org.scalaj"      %% "scalaj-http"                 % versions.scalaj

  val socrataHttpClient        = "com.socrata"     %% "socrata-http-client"         % versions.socrataHttp
  val socrataHttpCuratorBroker = "com.socrata"     %% "socrata-http-curator-broker" % versions.socrataHttp
  val socrataHttpJetty         = "com.socrata"     %% "socrata-http-jetty"          % versions.socrataHttp
  val socrataHttpServer        = "com.socrata"     %% "socrata-http-server"         % versions.socrataHttp

  val socrataThirdPartyUtils   = "com.socrata"     %% "socrata-thirdparty-utils"    % versions.thirdPartyUtils

  val soqlBrita                = "com.socrata"     %% "soql-brita"                  % versions.soqlBrita

  val soqlAnalyzer             = "com.socrata"     %% "soql-analyzer"               % versions.soqlReference
  val soqlStandaloneParser     = "com.socrata"     %% "soql-standalone-parser"      % versions.soqlReference
  val soqlStdLib               = "com.socrata"     %% "soql-stdlib"                 % versions.soqlReference
  val soqlTypes                = "com.socrata"     %% "soql-types"                  % versions.soqlReference

  val typesafeConfig           = "com.typesafe"     % "config"                      % versions.typesafeConfig

  object Test {
    val apacheCurator          = "org.apache.curator"     % "curator-test"          % versions.apacheCurator   % "test"
    val mockito                = "org.mockito"            % "mockito-all"           % versions.mockito         % "test"
    val mockServer             = "org.mock-server"        % "mockserver-netty"      % versions.mockServer      % "test"
    val scalaCheck             = "org.scalacheck"        %% "scalacheck"            % versions.scalaCheck      % "test,it"
    val scalaMock              = "org.scalamock"         %% "scalamock-scalatest-support" % versions.scalaMock % "test"
    val scalaTest              = "org.scalatest"         %% "scalatest"             % versions.scalaTest       % "test,it"
    val socrataThirdPartyUtils = "com.socrata"           %% "socrata-thirdparty-test-utils" % versions.thirdPartyUtils % "test"
    val springTest             = "org.springframework"    % "spring-test"           % versions.springTest      % "test"
    val wiremock               = "com.github.tomakehurst" % "wiremock"              % versions.wiremock        % "test"
  }
}