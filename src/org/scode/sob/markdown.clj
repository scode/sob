(ns org.scode.sob.markdown
  (:require [clojure.contrib.duck-streams :as duck-streams])
  (:import  [org.mozilla.javascript Context ScriptableObject]))

;;; Perform markdown->HTML conversion in a way compatible with the
;;; showdown JavaScriptimplementation of markdown. This is based
;;; heavily on Brian Carper's excellent blog post:
;;;
;;;    http://briancarper.net/blog/clojure-and-markdown-and-javascript-and-java-and
;;;
;;; I had to separate out the showdown load from the makeHtml() call
;;; or else the result would be generated code rather than the result
;;; of the final call. I'm also loading the showdown source via the
;;; ClassLoader.

(defn to-html [txt]
  (let [cx (Context/enter)
        scope (.initStandardObjects cx)
        input (Context/javaToJS txt scope)
        our-class-loader (.getClassLoader (.getClass to-html))
        showdown (duck-streams/slurp* (.getResourceAsStream our-class-loader "org/scode/sob/showdown-0.9.js"))
        converter-script "new Showdown.converter().makeHtml(input);"]
    (try
     (ScriptableObject/putProperty scope "input" input)
     (.evaluateString cx scope showdown "<cmd>" 1 nil)
     (let [result (.evaluateString cx scope converter-script "<cmd>" 1 nil)]
       (Context/toString result))
     (finally (Context/exit)))))
