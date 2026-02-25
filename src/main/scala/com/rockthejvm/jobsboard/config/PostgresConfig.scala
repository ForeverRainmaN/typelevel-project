package com.rockthejvm.jobsboard.config

import pureconfig.generic.derivation.default.*
import pureconfig.ConfigReader

final case class PostgresConfig(
    nThreads: Int,
    url: String,
    user: String,
    pass: String
) derives ConfigReader
