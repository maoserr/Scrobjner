import onnx
import onnxruntime as ort
from onnxruntime_extensions.tools import add_pre_post_processing_to_model as add_ppp

ONNX_MODEL = '../app/src/main/res/raw/samenc.onnx'
ONNX_MODEL_WITH_PRE_POST_PROCESSING = '../app/src/main/res/raw/samenc_enh.onnx'


if __name__ == "__main__":
    # ORT 1.14 and later support ONNX opset 18, which added antialiasing to the Resize operator.
    # Results are much better when that can be used. Minimum opset is 16.
    onnx_opset = 17
    model = onnx.load(ONNX_MODEL)
    inputs = [add_ppp.create_named_value("image", onnx.TensorProto.UINT8, ["num_bytes"])]

    model_input_shape = model.graph.input[0].type.tensor_type.shape
    w_in = model_input_shape.dim[-1].dim_value
    h_in = model_input_shape.dim[-2].dim_value

    pipeline = add_ppp.PrePostProcessor(inputs, onnx_opset)
    pipeline.add_pre_processing(
        [
            add_ppp.ConvertImageToBGR(),  # jpg/png image to BGR in HWC layout
            add_ppp.Resize((h_in, w_in)),
            add_ppp.ImageBytesToFloat(),  # Convert Y to float in range 0..1
            add_ppp.Unsqueeze([0, 1]),  # add batch and channels dim to Y so shape is {1, 1, h_in, w_in}
        ]
    )

    new_model = pipeline.run(model)
    onnx.save_model(new_model, ONNX_MODEL_WITH_PRE_POST_PROCESSING)
