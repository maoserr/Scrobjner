from typing import List, Optional

import onnx
from onnxruntime_extensions.tools import add_pre_post_processing_to_model as add_ppp
from onnxruntime_extensions.tools.pre_post_processing.step import Step

ONNX_MODEL = '../app/src/main/res/raw/samenc.onnx'
ONNX_MODEL_WITH_PRE_POST_PROCESSING = '../app/src/main/res/raw/samenc_enh.onnx'


class RGBAToRGB(Step):
    """
    Remove alpha channel
    """

    def __init__(self, name: Optional[str] = None):
        """
        Remove alpha channel from RGBA data
        :param name: Optional name
        """
        super().__init__(["rgba_data"], ["rgb_data"], name)
        self._axis = -1

    def _create_graph_for_step(self, graph: onnx.GraphProto, onnx_opset: int):
        input_type_str, input_shape_str = self._get_input_type_and_shape_strs(graph, 0)
        input_dims = input_shape_str.split(",")
        output_shape_tr = ",".join(input_dims[:2]+['3'])
        split_dim = input_dims[self._axis]

        if split_dim.isdigit():
            dim_value = int(split_dim)
            self._dim_value = dim_value

        split_outs = []
        for i in range(0, self._dim_value):
            split_outs.append(f"split_out_{i}")

        split_attr = f"axis = {self._axis}"
        if onnx_opset >= 18:
            # Split now requires the number of outputs to be specified even though that can be easily inferred...
            split_attr += f", num_outputs = {len(split_outs)}"

        removealpha_graph = onnx.parser.parse_graph(
            f"""\
            rgba_to_rgb ({input_type_str}[{input_shape_str}] {self.input_names[0]})
                => ({input_type_str}[{output_shape_tr}] {self.output_names[0]})  
            {{
                {','.join(split_outs)} = Split <{split_attr}> ({self.input_names[0]})
                {self.output_names[0]} = Concat <axis = {self._axis}> ({','.join(split_outs[:3])})
            }}
            """
        )

        return removealpha_graph


class BytesToFloat(Step):
    """
    Convert uint8 or float values in range 0..255 to floating point values in range 0..1
    """

    def __init__(self, rescale_factor: float = 1 / 255, name: Optional[str] = None):
        """
        Args:
            name: Optional step name. Defaults to 'ImageBytesToFloat'
        """
        super().__init__(["data"], ["float_data"], name)
        self.rescale_factor_ = rescale_factor

    def _create_graph_for_step(self, graph: onnx.GraphProto, onnx_opset: int):
        input_type_str, input_shape_str = self._get_input_type_and_shape_strs(graph, 0)
        if input_type_str == "uint8":
            optional_cast = f"""\
                input_f = Cast <to = 1> ({self.input_names[0]})
            """
        else:
            # no-op that optimizer will remove
            optional_cast = f"input_f = Identity ({self.input_names[0]})"

        byte_to_float_graph = onnx.parser.parse_graph(
            f"""\
            byte_to_float ({input_type_str}[{input_shape_str}] {self.input_names[0]}) 
                => (float[{input_shape_str}] {self.output_names[0]})
            {{
                f_scale = Constant <value = float[1] {{{self.rescale_factor_}}}>()

                {optional_cast}
                {self.output_names[0]} = Mul(input_f, f_scale)
            }}
            """
        )

        onnx.checker.check_graph(byte_to_float_graph)
        return byte_to_float_graph


if __name__ == "__main__":
    # ORT 1.14 and later support ONNX opset 18, which added antialiasing to the Resize operator.
    # Results are much better when that can be used. Minimum opset is 16.
    onnx_opset = 17
    model = onnx.load(ONNX_MODEL)
    inputs = [add_ppp.create_named_value("image", onnx.TensorProto.UINT8, ["h_in", "w_in", 4])]

    pipeline = add_ppp.PrePostProcessor(inputs, onnx_opset)
    pipeline.add_pre_processing(
        [
            RGBAToRGB(),
            BytesToFloat(),
            add_ppp.Resize((684, 1024), policy="not_larger"),
            add_ppp.ImageBytesToFloat(rescale_factor=1),  # Convert Y to float in range 0..1
        ]
    )

    new_model = pipeline.run(model)
    onnx.save_model(new_model, ONNX_MODEL_WITH_PRE_POST_PROCESSING)
