import onnx
from onnxruntime_extensions.tools import add_pre_post_processing_to_model as add_ppp

ONNX_MODEL = '../app/src/main/res/raw/samenc.onnx'
ONNX_MODEL_WITH_PRE_POST_PROCESSING = '../app/src/main/res/raw/samenc_enh.onnx'


if __name__ == "__main__":
    # ORT 1.14 and later support ONNX opset 18, which added antialiasing to the Resize operator.
    # Results are much better when that can be used. Minimum opset is 16.
    onnx_opset = 17
    model = onnx.load(ONNX_MODEL)
    inputs = [add_ppp.create_named_value("image", onnx.TensorProto.UINT8, ["num_bytes"])]

    pipeline = add_ppp.PrePostProcessor(inputs, onnx_opset)
    pipeline.add_pre_processing(
        [
            add_ppp.ConvertImageToBGR(),
            add_ppp.Resize((684, 1024), policy="not_larger"),
            add_ppp.ImageBytesToFloat(rescale_factor=1),  # Convert Y to float in range 0..1
        ]
    )

    new_model = pipeline.run(model)
    onnx.save_model(new_model, ONNX_MODEL_WITH_PRE_POST_PROCESSING)
