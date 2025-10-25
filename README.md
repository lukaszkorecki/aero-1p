# 1password reader for Aero

## What is it?

As the title says - this tiny library allows for reading data from 1Password and injecting them into configs read via [Aero](https://github.com/juxt/aero).

Using 1Password's [secret references](https://developer.1password.com/docs/cli/secret-references) we can dynamically inject secrets (and other data found in 1password), into your configuration at read time.

This is usually very helpful when combined with Aero's other features like profiles or dynamic includes, since 1Password CLI might not be available in outside of your local environment.

## Setup

First of all, make sure you [1Password CLI is configured and installed](https://developer.1password.com/docs/cli/get-started).

Then add this library to your `deps.edn`:


```edn
{:src ["src" "resources"]
 :deps {org.clojure/clojure {:mvn/version "1.12.0"} ;; required
         aero-1p/aero-1p {:git/url "https://github.com/lukaszkorecki/aero-1p.git"
                          :git/sha "...."}}

```

Follow usage guide below:

## Usage

Here's an example `config.edn` which uses secret references

> ![NOTE]
> Learn more about 1Password's secret references: https://developer.1password.com/docs/cli/secret-references/

```edn
;; Simple example, load secrts from default account:

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

Next step is to require `aero-1p` along with Aero and read the config:


```clojure
(ns app.config
  (:require [clojure.java.io :as io]
            [aero-1p.core] ;; register #op/secret tag
            [aero.core :as aero])

(def store (aero/read-config (io/resource "config.edn")))
```


## Notes

This library has been optimized so that only 1st secret fetch is a truly blocking operation, since 1Password will require authorization on first fetch. Once that happens all remaining secrets will be fetched using background threads.

Note that if your config reads secrets from multiple accounts, you'll need to confirm access using Touch ID once per each account.
