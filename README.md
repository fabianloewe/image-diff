# Image Diff

Work-in-progess

This is a simple CLI app to compare two images or all images contained in two directories with each other.
The comparasion for mulitple images is parallelised to increase performance.

## Usage

```
Usage: diff [<options>]

Options:
  -c, --cover=<path>     The first file or directory containing images to
                         compare
  -s, --stego=<path>     The second file or directory containing images to
                         compare
  -o, --output=<path>    The file or directory to write the diff(s) to
  -l, --cor-list=<path>  The file containing correspondences between the images
                         as a CSV file. It must contain two columns:
                         stego_image_filename and cover_image_filename. If not
                         provided, the images are compared in the order they
                         are found in the directories.
  --split                Whether to split the output in multiple JSON files
  --comparator=<text>    The comparator to use
  -F, --filter=<value>   The case-sensitive filters to apply to the
                         correspondences list (e.g. --filter column1=value1)
  -h, --help             Show this message and exit
```
