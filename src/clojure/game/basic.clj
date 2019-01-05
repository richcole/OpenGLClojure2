(ns game.basic
  (:require [clojure.java.io :as io])
  (:import (org.lwjgl.opengl GL GL11 GL20 GL30 GL15)
           (org.lwjgl.glfw GLFWErrorCallback GLFW Callbacks GLFWWindowSizeCallbackI)
           (org.lwjgl.system MemoryStack MemoryUtil)
           (java.nio IntBuffer FloatBuffer)
           ))

(defn shader-source [^Integer shader ^String shader-code]
  (GL20/glShaderSource shader shader-code))

(defn compile-shader [shader-type filename]
  (let [shader (GL20/glCreateShader shader-type)
        shader-code (slurp (io/resource filename))]
    (shader-source shader shader-code)
    (GL20/glCompileShader shader)
    (when (= GL11/GL_FALSE (GL20/glGetShaderi shader GL20/GL_COMPILE_STATUS))
      (throw (RuntimeException.
               (str "Unable to compile program: " (GL20/glGetShaderInfoLog shader 4096)))))
    shader))

(defn compile-program []
  (let [program (GL20/glCreateProgram)]
    (GL20/glAttachShader program (compile-shader GL20/GL_VERTEX_SHADER "simple-vert.glsl"))
    (GL20/glAttachShader program (compile-shader GL20/GL_FRAGMENT_SHADER "simple-frag.glsl"))
    (GL20/glLinkProgram program)
    (when (= GL11/GL_FALSE (GL20/glGetProgrami program GL20/GL_LINK_STATUS))
      (throw (RuntimeException.
               (str "Unable to link program: " (GL20/glGetProgramInfoLog program 4096)))))
    (GL20/glUseProgram program)
    program))

(def positions
  (float-array
    [ 0.0 0.0 0
      0.5 0.0 0
      0.5 0.5 0

      0.0 0.0 0
      0.0 0.5 0
      -0.5 0.5 0

      0.0 0.0 0
      -0.5 0.0 0
      -0.5 -0.5 0

      0.0 0.0 0
      0.0 -0.5 0
      0.5 -0.5 0]))

(def triangles
  (int-array
    [ 0 1 2 3 4 5 6 7 8 9 10 11 ]))

(GLFW/glfwInit)
(GLFW/glfwDefaultWindowHints)
(GLFW/glfwWindowHint GLFW/GLFW_VISIBLE GLFW/GLFW_FALSE)
(GLFW/glfwWindowHint GLFW/GLFW_RESIZABLE GLFW/GLFW_TRUE)

(def window (GLFW/glfwCreateWindow 300 300 "Hello World!" 0 0))

(GLFW/glfwMakeContextCurrent window)
(GLFW/glfwSwapInterval 1)
(GLFW/glfwShowWindow window)
(GL/createCapabilities)
(GL11/glClearColor 0.0 0.0 0.0 0.0)

(GLFW/glfwSetWindowSizeCallback
  window
  (reify GLFWWindowSizeCallbackI
    (invoke [this window width height]
      (GL11/glViewport 0 0 width height))))

(def vao (GL30/glGenVertexArrays))
(GL30/glBindVertexArray vao)

(def vbo (GL15/glGenBuffers))
(GL15/glBindBuffer GL15/GL_ARRAY_BUFFER vbo)
(GL15/glBufferData GL15/GL_ARRAY_BUFFER positions GL15/GL_STATIC_DRAW)

(def ibo (GL15/glGenBuffers))
(GL15/glBindBuffer GL15/GL_ELEMENT_ARRAY_BUFFER ibo)
(GL15/glBufferData GL15/GL_ELEMENT_ARRAY_BUFFER triangles GL15/GL_STATIC_DRAW)

(def program (compile-program))

(def ^Integer position_attribute (GL20/glGetAttribLocation program "positions"))
(println "Position attribute" position_attribute)
(GL20/glVertexAttribPointer position_attribute 3 GL11/GL_FLOAT false 0 0)
(GL20/glEnableVertexAttribArray position_attribute)

(defn main-loop [window]
  (GL30/glBindVertexArray vao)
  (GL20/glEnableVertexAttribArray position_attribute)
  (GL15/glBindBuffer GL15/GL_ELEMENT_ARRAY_BUFFER ibo)
  (GL11/glDrawElements GL11/GL_TRIANGLES 12 GL11/GL_UNSIGNED_INT 0)
  ; (GL11/glDrawArrays GL11/GL_TRIANGLES 0 12)
  (GLFW/glfwSwapBuffers window)
  (GLFW/glfwPollEvents))

(while (not (GLFW/glfwWindowShouldClose window))
  (main-loop window))



