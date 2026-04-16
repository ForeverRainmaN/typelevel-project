package com.rockthejvm.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class PostgresTestConfig(
    dbUrl: String,
    dbUser: String,
    dbPassword: String
) derives ConfigReader
