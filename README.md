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
  --split                     Whether to split the output in multiple JSON
                              files (default: false)
  --comparator=<text>         The comparator to use
  -F, --filter=<value>        The case-sensitive filters to apply to the
                              correspondences list (e.g. --filter
                              column1=value1)
  -P, --comparator-param=<value>
                              The parameters to pass to the comparators in the
                              format comp1.param1=value
  -L, --max-value-len=<int>   The maximum length of the value in the diff
                              output (default: 100)
  --parallel / --no-parallel  Whether to compare images in parallel. WARNING:
                              This may consume a lot of memory and potentially
                              crash the program. (default: false)
  --truncate / --no-truncate  Whether to truncate the diff values in the output
                              to the maximum length (default: true)
  -h, --help                  Show this message and exit
```

### extract

```
Usage: image-diff extract [<options>]

Options:
  -s, --stego=<path>             The second file or directory containing images
                                 to compare
  -o, --output=<path>            The directory to write the extracted data to.
                                 There will be one plain file per image per
                                 extractor and a JSON file containing paths to
                                 the extracted data.
  -l, --cor-list=<path>          The file containing correspondences between
                                 the images as a CSV file. Only one column
                                 named stego_image_filename must be present.
                                 This can be handy with the -F option to filter
                                 the correspondences list.
  --extractor=<text>             The comparator to use
  -F, --filter=<value>           The case-sensitive filters to apply to the
                                 correspondences list on a certain column (e.g.
                                 --filter column1=value1)
  -P, --extractor-param=<value>  The parameters to pass to the extractors in
                                 the format extr1.param1=value
  --parallel / --no-parallel     Whether to extract from images in parallel.
                                 WARNING: This may consume a lot of memory and
                                 potentially crash the program. (default:
                                 false)
  -h, --help                     Show this message and exit
```