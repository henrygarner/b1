(defproject b1 "0.3.0"

  :description "Simple data visualization in Clojure(Script)."
  :url "https://github.com/henrygarner/b1"
  :license {:name "BSD"
            :url "http://www.opensource.org/licenses/BSD-3-Clause"}
  
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-iterate "0.96"]]

  :profiles {:dev {:dependencies [[midje "1.5.0"]]}}

  :plugins [[com.keminglabs/cljx "0.2.1"]
            [lein-cljsbuild "0.3.1"]
            [lein-midje "3.0.0"]]

  :source-paths ["src/clj" "src/cljs"
                 "target/generated/clj"
                 "target/generated/cljs"]

  :cljx {:builds [{:source-paths ["src"]
                   :output-path "target/generated/clj"
                   :rules cljx.rules/clj-rules}

                  {:source-paths ["src"]
                   :output-path "target/generated/cljs"
                   :extension "cljs"
                   :rules cljx.rules/cljs-rules}]}

  :hooks [cljx.hooks])
