(defproject b1 "0.3.3-SNAPSHOT"

  :description "Simple data visualization in Clojure(Script)."
  :url "https://github.com/henrygarner/b1"
  :license {:name "BSD"
            :url "http://www.opensource.org/licenses/BSD-3-Clause"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3308" :scope "provided"]
                 [clj-iterate "0.96"]
                 [org.clojure/core.match "0.2.0-alpha12"]]

  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-midje "3.1.3"]]

  :profile {:dev {:dependencies [[midje "1.6.3"]]}}

  :source-paths ["src"]

  :cljsbuild
  {:builds
   [{:source-paths ["src"]
     :compiler {:output-to "target/main.js"
                :pretty-print true}}]})
