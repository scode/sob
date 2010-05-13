(ns org.scode.sob
  (:gen-class)
  (:require [org.scode.sob.markdown :as markdown]
            [clojure.contrib.command-line :as cmdline]
            [clojure.contrib.logging :as logging]
            [ring.adapter.jetty])
  (:use [org.scode.sob.repos :as repos]))

(defn die [msg]
  (logging/fatal msg)
  (System/exit 1))

(defn make-blog-app
  [base path]
  (let [repos (repos/new path)]
    (fn [req]
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (str req)})))

(defn serve-blog
  [port app]
  (ring.adapter.jetty/run-jetty app {:port port}))

(defn -main [& args]
  (cmdline/with-command-line args
    "sob - scode's own blog"
    [[port     "Select listen port" 8081]
     [base     "Select base URI path" "/"]
     paths]
    (if (> 1 (count paths))
      (die "only one file system path currently supported"))
    (if (empty? paths)
      (die "must specify the path to a blog"))
    (logging/info (str "starting sob on *:" port base))
    (serve-blog port (make-blog-app base (first paths)))))



