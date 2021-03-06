/*
 * Copyright 2020 Precog Data
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package quasar.datasource.kafka

import slamdata.Predef._

import argonaut._

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class TunnelConfigSpec extends Specification with ScalaCheck {
  import TunnelConfig.Auth._

  "json codec" >> {
    "lawful for password" >> prop { params: (String, Int, String, String)  =>
      CodecJson.codecLaw(TunnelConfig.codecTunnelConfig)(TunnelConfig(params._1, params._2, params._3, Some(Password(params._4))))
    }
    "lawful for identity" >> prop { params: (String, Int, String, String, Option[String]) =>
      CodecJson.codecLaw(TunnelConfig.codecTunnelConfig)(TunnelConfig(
        params._1,
        params._2,
        params._3,
        Some(Identity(params._4, params._5))))
    }
  }

  "TunnelConfig" >> {
    val identity = TunnelConfig.Auth.Identity("private_key", Some("aPassphrase"))
    val password = TunnelConfig.Auth.Password("aPassword")

    "getPassword is empty string if no auth" >> {
      TunnelConfig("localhost", 22222, "root", None).getPassword must beEmpty
    }

    "getPassphrase is empty string if no auth" >> {
      TunnelConfig("localhost", 22222, "root", None).getPassphrase must beEmpty
    }

    "getPassword is empty string if auth is identity" >> {
      TunnelConfig("localhost", 22222, "root", Some(identity)).getPassword must beEmpty
    }

    "getPassphrase is empty string if auth is password" >> {
      TunnelConfig("localhost", 22222, "root", Some(password)).getPassphrase must beEmpty
    }

    "getPassword is returns Password's password" >> {
      TunnelConfig("localhost", 22222, "root", Some(password)).getPassword must_=== "aPassword"
    }

    "getPassphrase is returns Identity's passphrase" >> {
      TunnelConfig("localhost", 22222, "root", Some(identity)).getPassphrase must_=== "aPassphrase"
    }
  }
}
