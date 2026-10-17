#!/usr/bin/env python3
"""Recompute the checked-in reference/port screenshot comparisons (requires Pillow)."""
import argparse
import json
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw


def compare(reference, port, include, exclude):
    with Image.open(reference) as original, Image.open(port) as actual:
        if original.size != actual.size:
            raise ValueError('Screenshot dimensions differ')
        mask = Image.new('L', original.size)
        painter = ImageDraw.Draw(mask)
        for rectangles, value in (([include], 255), (exclude, 0)):
            for x, y, width, height in rectangles:
                if width <= 0 or height <= 0 or x < 0 or y < 0 or x + width > original.width or y + height > original.height:
                    raise ValueError('Comparison rectangle outside screenshot')
                painter.rectangle((x, y, x + width - 1, y + height - 1), fill=value)
        difference = ImageChops.difference(original.convert('RGB'), actual.convert('RGB'))
        red, green, blue = difference.split()
        different = ImageChops.lighter(ImageChops.lighter(red, green), blue).point(lambda value: 255 if value else 0)
        return mask.histogram()[255], ImageChops.multiply(different, mask).histogram()[255]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('report', nargs='?', type=Path, default=Path(__file__).resolve().parents[1] / 'docs/selector-pixel-comparison.json')
    args = parser.parse_args()
    report = json.loads(args.report.read_text())
    results = {}
    for name, specification in report.items():
        if not isinstance(specification, dict) or 'include' not in specification:
            continue
        images = args.report.parent / 'images'
        suffix = name.replace('_', '-') + '.png'
        reference = args.report.parent / specification['reference_image'] if 'reference_image' in specification else images / ('reference-' + suffix)
        port = args.report.parent / specification['port_image'] if 'port_image' in specification else images / ('port-' + suffix)
        compared, different = compare(reference, port, specification['include'], specification.get('exclude', []))
        results[name] = {'pixels_compared': compared, 'different_pixels': different}
    print(json.dumps(results, indent=2))
    return int(any(result['different_pixels'] for result in results.values()))


if __name__ == '__main__':
    raise SystemExit(main())
