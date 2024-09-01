import tensorflow as tf
import time

def main():
    a=tf.lite.Interpreter(r'C:\Users\maose\My Drive\Code\GooglePlay\encoder.tflite')
    b=(a.get_signature_runner())
    start = time.time()
    c=b(args_0=tf.constant([1.0], shape=(1024,1024,3),
                           dtype=tf.float32))
    print(f'{time.time() - start}')
    c=b(args_0=tf.constant([1.0], shape=(1024,1024,3),
                           dtype=tf.float32))
    print(f'{time.time() - start}')






if __name__ == "__main__":
    main()
