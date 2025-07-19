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

and you're all set.

## Usage

Let's say you have this config map:

```edn
;; very simple example

{:github {:org "test"
          ;; read from default account e.g. the personal one
          :pat #op/secret "op://Private/github/test"}

 :aws {:access-key-id #op/secret {:account "mycompany.1password.com"
                                  :path "op://Employee/aws/test/access-key-id"}

       :access-secret-key #op/secret {:account "mycompany.1password.com"
                                      :path "op://Employee/aws/test/secrets-access-key"}
       }
 }


;; you can use #ref to simplify the config a bit

{
 :op-acc-id "mycompany.1password.com"
 :github {:org "test"
          ;; read from default account e.g. the personal one
          :pat #op/secret {:account #ref [:op-acc-id]
                           :path "op://Private/github/test"}

          :repo #op/secret {:account #ref [:op-acc-id]
                            :path "op://Private/github/test/repo"}}

 :aws {:access-key-id #op/secret {:account #ref [:op-acc-id]
                                  :path "op://Employee/aws/test/access-key-id"}

       :secret-access-key #op/secret {:account #ref [:op-acc-id]
                                      :path "op://Employee/aws/test/secrets-access-key"}
       }
 }


```

you can load it this way:


```clojure
(require
 '[aero.core :as aero]
 '[clojrue.java.io :as io]
 '[aero-1p.core])

(def store (aero/read-config (io/resource "config.edn")))
```


## Tips & Notes

- you can find secret refrences by using the `op` cli itself, or using 'copy secret reference' options from secret's context menu
- because each secret is read individually via shell invokations, given sufficient number of secrets config load might be slow, this is something that might improve in the future, we will see
  - you can alternatively use `op inject` which loads secrets in one go
