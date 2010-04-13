(ns org.scode.sob
  (:gen-class)
  (:require [org.scode.sob.markdown :as markdown]
            [clojure.contrib.command-line :as cmdline]
            [clojure.contrib.logging :as logging]
            [ring.adapter.jetty]))

(defn die [msg]
  (logging/fatal msg)
  (System/exit 1))

(defn make-blog-app
  [port base path]
  (fn [req]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body "not implemented"}))

(defn serve-blog
  "Start serving a blog at http://*:port/base."
  [port base path]
  (logging/info (str "starting sob on *:" port base ", serving " path))
  (ring.adapter.jetty/run-jetty (make-blog-app port base path) {:port port}))

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
    (serve-blog port base (first paths))))



