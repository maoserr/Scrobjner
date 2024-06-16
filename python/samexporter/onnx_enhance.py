from typing import List, Optional

import onnx
from onnx.helper import make_tensor, make_node, make_graph, make_tensor_value_info
from onnxruntime_extensions.tools import add_pre_post_processing_to_model as add_ppp
from onnxruntime_extensions.tools.pre_post_processing.step import Step, Debug
from onnxsim import simplify, model_info

ONNX_MODEL_ENC = '../app/src/main/res/raw/samenc.onnx'
ONNX_MODEL_ENC_WITH_PRE_POST_PROCESSING = '../app/src/main/res/raw/samenc_enh.onnx'

ONNX_MODEL_DEC = '../app/src/main/res/raw/samdec.onnx'
ONNX_MODEL_DEC_WITH_POST_PROCESSING = '../app/src/main/res/raw/samdec_enh.onnx'


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
        output_shape_tr = ",".join(input_dims[:2] + ['3'])
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
        print(onnx.printer.to_text(removealpha_graph))
        return removealpha_graph


class BytesToFloat(Step):
    """
    Cast uint8 to float
    """

    def __init__(self, name: Optional[str] = None):
        """
        Args:
            name: Optional step name. Defaults to 'ImageBytesToFloat'
        """
        super().__init__(["data"], ["float_data"], name)

    def _create_graph_for_step(self, graph: onnx.GraphProto, onnx_opset: int):
        input_type_str, input_shape_str = self._get_input_type_and_shape_strs(graph, 0)

        byte_to_float_graph = onnx.parser.parse_graph(
            f"""\
            byte_to_float ({input_type_str}[{input_shape_str}] {self.input_names[0]}) 
                => (float[{input_shape_str}] {self.output_names[0]})
            {{
                {self.output_names[0]} = Cast <to = 1> ({self.input_names[0]})
            }}
            """
        )
        print(onnx.printer.to_text(byte_to_float_graph))
        onnx.checker.check_graph(byte_to_float_graph)
        return byte_to_float_graph


class MaskToRGBA(Step):
    def __init__(self, name: Optional[str] = None):
        """
        Remove alpha channel from RGBA data
        :param name: Optional name
        """
        super().__init__(["mask_data", "orig_im_size"], ["rgba_data"], name)
        self._axis = -1

    def _create_graph_for_step(self, graph: onnx.GraphProto, onnx_opset: int):
        _, input_shape_str = self._get_input_type_and_shape_strs(graph, 0)
        inpt_shape = input_shape_str.split(",")

        shape_cast = make_node(
            "Cast",
            inputs=[self.input_names[1]],
            outputs=["im_size"],
            to=onnx.TensorProto.INT64
        )

        sq_ax = make_node(
            "Constant",
            inputs=[],
            outputs=["sq_axe"],
            value=make_tensor(name="const_tensor", data_type=onnx.TensorProto.INT64, dims=[1], vals=[-1]),
        )

        r = make_node(
            "ConstantOfShape",
            inputs=["im_size"],
            outputs=["red_2d"],
            value=make_tensor("value", onnx.TensorProto.UINT8, [1], [255]),
        )
        r_sq = make_node(
            "Unsqueeze",
            inputs=["red_2d", "sq_axe"],
            outputs=["red"]
        )

        g = make_node(
            "ConstantOfShape",
            inputs=["im_size"],
            outputs=["green_2d"],
            value=make_tensor("value", onnx.TensorProto.UINT8, [1], [0]),
        )
        g_sq = make_node(
            "Unsqueeze",
            inputs=["green_2d", "sq_axe"],
            outputs=["green"]
        )

        b = make_node(
            "ConstantOfShape",
            inputs=["im_size"],
            outputs=["blue_2d"],
            value=make_tensor("value", onnx.TensorProto.UINT8, [1], [0]),
        )
        b_sq = make_node(
            "Unsqueeze",
            inputs=["blue_2d", "sq_axe"],
            outputs=["blue"]
        )

        mask_sq = make_node(
            "Unsqueeze",
            inputs=[self.input_names[0], "sq_axe"],
            outputs=["mask_sq"]
        )
        concat = make_node(
            "Concat",
            inputs=["mask_sq", "green", "blue", "mask_sq"],
            outputs=["rgba_data"],
            axis=-1
        )

        # ,  b, b_sq, mask_sq, concat
        graph = make_graph([
            shape_cast,
            sq_ax,
            r,
            r_sq,
            g,
            g_sq,
            b,
            b_sq,
            mask_sq,
            concat
        ], "mask_to_rgba",
                           [
                               make_tensor_value_info(self.input_names[0], onnx.TensorProto.UINT8, inpt_shape),
                               make_tensor_value_info(self.input_names[1], onnx.TensorProto.FLOAT, [2])
                           ],
                           [
                               make_tensor_value_info(self.output_names[0], onnx.TensorProto.UINT8,
                                                      inpt_shape[0:2] + [4])
                           ]
                           )
        print(onnx.printer.to_text(graph))
        return graph


def run_enc_pipe():
    model = onnx.load(ONNX_MODEL_ENC)
    inputs = [add_ppp.create_named_value("image", onnx.TensorProto.UINT8, ["h_in", "w_in", 4])]

    pipeline = add_ppp.PrePostProcessor(inputs, onnx_opset)
    pipeline.add_pre_processing(
        [
            RGBAToRGB(),
            add_ppp.Resize(1024, policy="not_larger"),
            BytesToFloat(),
        ]
    )

    new_model = pipeline.run(model)
    # convert model
    model_simp, check = simplify(new_model)

    assert check, "Simplified ONNX model could not be validated"
    model_info.print_simplifying_info(new_model, model_simp)
    onnx.save_model(model_simp, ONNX_MODEL_ENC_WITH_PRE_POST_PROCESSING)


def run_dec_pipe():
    model = onnx.load(ONNX_MODEL_DEC)
    inputs = [
        add_ppp.create_named_value("image_embeddings", onnx.TensorProto.FLOAT, [1, 256, 64, 64]),
        add_ppp.create_named_value("point_coords", onnx.TensorProto.FLOAT, [1, "num_points", 2]),
        add_ppp.create_named_value("point_labels", onnx.TensorProto.FLOAT, [1, "num_points"]),
        add_ppp.create_named_value("mask_input", onnx.TensorProto.FLOAT, [1, 1, 256, 256]),
        add_ppp.create_named_value("has_mask_input", onnx.TensorProto.FLOAT, [1]),
        add_ppp.create_named_value("orig_im_size", onnx.TensorProto.FLOAT, [2]),
    ]

    pipeline = add_ppp.PrePostProcessor(inputs, onnx_opset)
    pipeline.add_post_processing(
        [
            add_ppp.Squeeze([0, 1]),
            add_ppp.FloatToImageBytes(),
            MaskToRGBA()
        ]
    )

    new_model = pipeline.run(model)
    # convert model
    model_simp, check = simplify(new_model)

    assert check, "Simplified ONNX model could not be validated"
    model_info.print_simplifying_info(new_model, model_simp)
    onnx.save_model(model_simp, ONNX_MODEL_DEC_WITH_POST_PROCESSING)


if __name__ == "__main__":
    # ORT 1.14 and later support ONNX opset 18, which added antialiasing to the Resize operator.
    # Results are much better when that can be used. Minimum opset is 16.
    onnx_opset = 17
    run_enc = False
    run_dec = True
    if run_enc:
        run_enc_pipe()

    if run_dec:
        run_dec_pipe()
