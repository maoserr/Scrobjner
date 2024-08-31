# export LD_LIBRARY_PATH=/home/maoserr/miniconda3/envs/scrobjner/lib/
# export PYTHONPATH=$PWD

def encoder():
    import torch
    from segment_anything import sam_model_registry
    from samexporter.mobile_encoder.setup_mobile_sam import setup_model
    from samexporter.onnx_utils import ImageEncoderOnnxModel
    model_type = "mobile"
    checkpoint = "mobile_sam.pt"
    use_preprocess = True
    gelu_approximate = False
    print("Loading model...")
    if model_type == "mobile":
        checkpoint = torch.load(checkpoint, map_location="cpu")
        sam = setup_model()
        sam.load_state_dict(checkpoint, strict=True)
    else:
        sam = sam_model_registry[model_type](checkpoint=checkpoint)

    onnx_model = ImageEncoderOnnxModel(
        model=sam,
        use_preprocess=use_preprocess,
        pixel_mean=[123.675, 116.28, 103.53],
        pixel_std=[58.395, 57.12, 57.375],
    )

    if gelu_approximate:
        for _, m in onnx_model.named_modules():
            if isinstance(m, torch.nn.GELU):
                m.approximate = "tanh"

    image_size = sam.image_encoder.img_size
    if use_preprocess:
        dummy_input = (torch.randn((image_size, image_size, 3), dtype=torch.float),)
        dynamic_axes = {
            "input_image": {0: "image_height", 1: "image_width"},
        }
    else:
        dummy_input = {
            "input_image": torch.randn(
                (1, 3, image_size, image_size), dtype=torch.float
            )
        }
        dynamic_axes = None

    import torch
    import ai_edge_torch

    # Convert and serialize PyTorch model to a tflite flatbuffer. Note that we
    # are setting the model to evaluation mode prior to conversion.
    edge_model = ai_edge_torch.convert(onnx_model.eval(), dummy_input)
    edge_model.export("encoder.tflite")

def decoder():
    import torch
    from mobile_sam import sam_model_registry
    from samexporter.mobile_encoder.util_onnx import SamOnnxModel

    model_type = "vit_t"
    checkpoint = "mobile_sam.pt"
    gelu_approximate = True

    print("Loading model...")
    sam = sam_model_registry[model_type](checkpoint=checkpoint)

    onnx_model = SamOnnxModel(
        model=sam,
        return_single_mask=False,
        use_stability_score=True,
        return_extra_metrics=False,
    )

    if gelu_approximate:
        for _, m in onnx_model.named_modules():
            if isinstance(m, torch.nn.GELU):
                m.approximate = "tanh"

    embed_dim = sam.prompt_encoder.embed_dim
    embed_size = sam.prompt_encoder.image_embedding_size
    mask_input_size = [4 * x for x in embed_size]
    dummy_inputs = (
        torch.randn(
            1, embed_dim, *embed_size, dtype=torch.float
        ),
        torch.randint(
            low=0, high=1024, size=(1, 5, 2), dtype=torch.float
        ),
        torch.randint(
            low=0, high=4, size=(1, 5), dtype=torch.float
        ),
        torch.randn(1, 1, *mask_input_size, dtype=torch.float),
        torch.tensor([1], dtype=torch.float),
        torch.tensor([1500, 2250], dtype=torch.float),
    )

    import ai_edge_torch

    # Convert and serialize PyTorch model to a tflite flatbuffer. Note that we
    # are setting the model to evaluation mode prior to conversion.
    edge_model = ai_edge_torch.convert(onnx_model.eval(), dummy_inputs)
    edge_model.export("decoder.tflite")

if __name__ == "__main__":
    decoder()
