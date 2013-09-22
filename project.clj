(defproject masterplan "0.1.0-SNAPSHOT"
  :description "Task management web-app."
  :url "http://example.com/FIXME"
  :license {:name "AGPL v3"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1889"]
                 [com.cemerick/piggieback "0.1.0"]
                 [fogus/bacwn "0.3.0"]
                 [org.clojure/core.async "0.1.0-SNAPSHOT"]
                 [prismatic/dommy "0.1.2"]]
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :hooks [leiningen.cljsbuild]
  :plugins [[lein-cljsbuild "0.3.3"]]
  :cljsbuild {
                                        ; Configure the REPL support; see the README.md file for more details.
              :repl-listen-port 9011
              :repl-launch-commands
                                        ; Launch command for connecting the page of choice to the REPL.
                                        ; Only works if the page at URL automatically connects to the REPL,
                                        ; like http://localhost:3000/repl-demo does.
                                        ;     $ lein trampoline cljsbuild repl-launch firefox <URL>
              {"firefox" ["firefox"
                          :stdout ".repl-firefox-out"
                          :stderr ".repl-firefox-err"]
               }
              :builds {
                                        ; This build has the lowest level of optimizations, so it is
                                        ; useful when debugging the app.
                       :dev
                       {:source-paths ["src"]
                        :jar true
                        :compiler {:output-to "resources/public/js/main-debug.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}}})
