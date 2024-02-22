# Image Diff

Work-in-progess

This is a simple CLI app to compare two images or all images contained in two directories with each other.
The comparasion for mulitple images can be parallelised to increase performance
but this may consume a lot of memory and potentially crash the program.

## Usage

```
Usage: image-diff [<options>] <command> [<args>]...

Options:
  -h, --help  Show this message and exit

Commands:
  diff
  extract
```

### diff

```
Usage: image-diff diff [<options>]

Metadata Comparator Options:

  Options for the metadata comparator.

  --ignore-nulls / --no-ignore-nulls
                             Whether to ignore null values in the stego image.
  -L, --max-value-len=<int>  The maximum length of the value in the diff
                             output. 0 means no limit. (default: 100)

Composite Comparator Options:

  Options for the composite comparator.

  --composite=<value>   The type of compositing to use.
  --preprocess=<value>  A preprocessing operation to apply to the images before
                        compositing them.
  --mode=<value>        The mode for applying the compositing operation.
  -I, --default-image-type=<text>
                        The default image type to use for the output. This will
                        be used if the image type cannot be determined from the
                        input images. (Default: png)

Options:
  -c, --cover=<path>          The first file or directory containing images to
                              compare
  -s, --stego=<path>          The second file or directory containing images to
                              compare
  -o, --output=<path>         The file or directory to write the diff(s) to
  -l, --cor-list=<path>       The file containing correspondences between the
                              images as a CSV file. It must contain two
                              columns: stego_image_filename and
                              cover_image_filename. If not provided, the images
                              are compared in the order they are found in the
                              directories.
  --comparator=<text>         The comparator to use
  -F, --filter=<value>        The case-sensitive filters to apply to the
                              correspondences list (e.g. --filter
                              column1=value1)
  --parallel / --no-parallel  Whether to compare images in parallel. WARNING:
                              This may consume a lot of memory and potentially
                              crash the program. (default: false)
  -h, --help                  Show this message and exit
```

### extract

```
Usage: image-diff extract [<options>]

Lsb Extractor Options:

  Options for the lsb extractor.

  --color-channels=<value>  The color channels to extract the least significant
                            bits from. Any combination of a, r, g and b.
                            (Default: rgb)
  --bits-per-channel=<int>  The number of bits per color channel to extract.
                            (Default: 8)

Options:
  -s, --stego=<path>          The file or directory containing images to
                              compare
  -o, --output=<path>         The directory to write the extracted data to.
                              There will be one plain file per image per
                              extractor and a JSON file containing paths to the
                              extracted data.
  -l, --cor-list=<path>       The file containing correspondences between the
                              images as a CSV file. Only one column named
                              stego_image_filename must be present. This can be
                              handy with the -F option to filter the
                              correspondences list.
  --extractor=<text>          The extractor to use
  -F, --filter=<value>        The case-sensitive filters to apply to the
                              correspondences list on a certain column (e.g.
                              --filter column1=value1)
  --parallel / --no-parallel  Whether to extract from images in parallel.
                              WARNING: This may consume a lot of memory and
                              potentially crash the program. (default: false)
  -h, --help                  Show this message and exit
```