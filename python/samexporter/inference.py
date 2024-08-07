import argparse
import sys
import json
import pathlib

sys.path.append(".")

import cv2
import numpy as np

from samexporter.sam_onnx import SegmentAnythingONNX

enhance = False
enhance_dec = True
if enhance:
    enc_mod = r"../app/src/main/res/raw/samenc_enh.onnx"
else:
    enc_mod = r"../app/src/main/res/raw/samenc.onnx"


if enhance_dec:
    dec_mod = r"../app/src/main/res/raw/samdec_enh.onnx"
else:
    dec_mod = r"../app/src/main/res/raw/samdec.onnx"

def str2bool(v):
    return v.lower() in ("true", "1")


argparser = argparse.ArgumentParser()
argparser.add_argument(
    "--encoder_model",
    type=str,
    default=enc_mod,
    help="Path to the ONNX encoder model",
)
argparser.add_argument(
    "--decoder_model",
    type=str,
    default=dec_mod,
    help="Path to the ONNX decoder model",
)
argparser.add_argument(
    "--image",
    type=str,
    default=r"car.jpg",
    help="Path to the image",
)
argparser.add_argument(
    "--prompt",
    type=str,
    default=r"car_prompt.json",
    help="Path to the image",
)
argparser.add_argument(
    "--output",
    type=str,
    default="output.jpg",
    help="Path to the output image",
)
argparser.add_argument(
    "--show",
    default=True,
    action="store_true",
    help="Show the result",
)

if __name__ == "__main__":
    args = argparser.parse_args()

    model = SegmentAnythingONNX(
        args.encoder_model,
        args.decoder_model,
    )

    image = cv2.imread(args.image)
    # image = cv2.rotate(image, cv2.ROTATE_90_COUNTERCLOCKWISE)

    prompt = json.load(open(args.prompt))

    embedding = model.encode(image, enhance)
    masks, lowm = model.predict_masks(embedding, prompt, enhance_dec)

    if enhance_dec:
        visualized = masks
        # visualized = np.squeeze(np.transpose(lowm, (2,3,1,0)),3)
        print(masks.shape)
    else:
        # Save the masks as a single image.
        mask = np.zeros((masks.shape[2], masks.shape[3], 3), dtype=np.uint8)
        for m in masks[0, :, :, :]:
            mask[m > 0.0] = [255, 0, 0]

        # Binding image and mask
        visualized = np.squeeze(np.transpose(masks, (2,3,1,0)),3)
        # visualized = cv2.addWeighted(image, 0.5, mask, 0.5, 0)

        # Draw the prompt points and rectangles.
        # for p in prompt:
        #     if p["type"] == "point":
        #         color = (
        #             (0, 255, 0) if p["label"] == 1 else (0, 0, 255)
        #         )  # green for positive, red for negative
        #         cv2.circle(visualized, (p["data"][0], p["data"][1]), 10, color, -1)
        #     elif p["type"] == "rectangle":
        #         cv2.rectangle(
        #             visualized,
        #             (p["data"][0], p["data"][1]),
        #             (p["data"][2], p["data"][3]),
        #             (0, 255, 0),
        #             2,
        #         )

    if args.output is not None:
        pathlib.Path(args.output).parent.mkdir(parents=True, exist_ok=True)
        cv2.imwrite(args.output, visualized)

    if args.show:
        cv2.imshow("Result", visualized)
        cv2.waitKey(0)
