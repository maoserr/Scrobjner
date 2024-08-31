import tensorflow as tf

def main():
    a=tf.lite.Interpreter(r'C:\Users\maose\My Drive\Code\GooglePlay\encoder.tflite')
    b=(a.get_signature_runner())
    c=b(args_0=tf.constant([1.0], shape=(1024,1024,3),
                           dtype=tf.float32))








if __name__ == "__main__":
    main()
