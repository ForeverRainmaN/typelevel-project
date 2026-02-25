package com.rockthejvm.jobsboard.config

import pureconfig.generic.derivation.default.*
import pureconfig.ConfigReader

final case class AppConfig(emberConfig: EmberConfig, postgresConfig: PostgresConfig)
    derives ConfigReader
