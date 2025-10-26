# 1Password integration for Aero

## What is it?

This tiny library allows for reading data from 1Password and injecting them into configs read via [Aero](https://github.com/juxt/aero).

Using 1Password's [secret references](https://developer.1password.com/docs/cli/secret-references) we can dynamically inject secrets (and other data found in 1Password), into your configuration at read time.

This is usually very helpful when combined with Aero's other features like profiles or dynamic includes, since 1Password CLI might not be available in outside of your local environment.

## Setup

First of all, make sure you have [1Password CLI configured and installed](https://developer.1password.com/docs/cli/get-started).

Then add this library to your `deps.edn`:


```edn
{:src ["src" "resources"]
 :deps {org.clojure/clojure {:mvn/version "1.12.0"} ;; min required
        aero/aero {....}
        aero-1p/aero-1p {:git/url "https://github.com/lukaszkorecki/aero-1p.git"
                         :git/sha "...."}}

```

### Supported platforms

- [x] Clojure on the JVM
- [ ] Babashka (most likely works out of the box, needs checking)
- [ ] ~~ClojureScript~~ - PRs welcomed, I don't use Cljs myself


## Usage


Here's an example `config.edn` which uses secret references

> [!NOTE]
> Learn more about 1Password's secret references: https://developer.1password.com/docs/cli/secret-references/

```edn
;; Simple example, load secrets from default account:

{:github {:org "test"
          ;; read from default account e.g. the personal one
          :pat #op/secret "op://Private/github/test"}

 :openai-api-key #op/secret "op://Private/OpenAi/token"}
```


A more complicated example, where secrets are loaded from a company/org account:

```edn
{:op-acc-id "mycompany.1password.com"
 :github {:org "test"
          ;; read from default account e.g. the personal one
          :pat #op/secret {:account #ref [:op-acc-id]
                           :path "op://Private/github/test"}

          :repo #op/secret {:account #ref [:op-acc-id]
                            :path "op://Private/github/test/repo"}}

 :aws {:access-key-id #op/secret {:account #ref [:op-acc-id]
                                  :path "op://Employee/aws/test/access-key-id"}
       :secret-access-key #op/secret {:account #ref [:op-acc-id]
                                      :path "op://Employee/aws/test/secrets-access-key"}}}

```

> [!NOTE]
> You can reference multiple accounts in one config, e.g. pull some from your default account (my.1password.com)
> and some from a team/company ccount (<yourcompany>.1password.com).

Next step is to require `aero-1p` along with Aero and read the config in your application


```clojure
(ns app.config
  (:require [clojure.java.io :as io]
            [aero-1p.core] ;; register #op/secret tag
            [aero.core :as aero])

(def store (aero/read-config (io/resource "config.edn")))
```

This is a simplified example, in an application which is deployed to the cloud 🌧️, secrets would be injected as environment variables in that environment, but read from 1Password locally. This is where [Aero's profiles](https://github.com/juxt/aero#profile) are very handy:


```clojure
{:application {:api-key #profile {:development #op/secret "op://Private/some-vendor/api-key"
                                  :production #env "API_KEY"}}}
```

and in the application code:

```clojure
;; you get the idea
(defn get-profile []
  (if (System/getenv "IN_THE_CLOUD")
    :production
    :development))

(def config
  (aero/read-config (io/resource "config.edn") (get-profile)))
```


## Notes

This library has been optimized so that only 1st secret fetch is a truly blocking operation, since 1Password will require authorization on first fetch. Once that happens all remaining secrets will be fetched using background threads.

Note that if your config reads secrets from multiple accounts, you'll need to confirm access using Touch ID once per each account.
