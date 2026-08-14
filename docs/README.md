<!--
  ~ SPDX-FileCopyrightText: 2017-2026 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
  -->

# Developer setup

Run these commands from the project root folder.

## Python

1. Create a virtual environment:
    - Unix/macOS: `python3 -m venv .venv`
    - Windows: `py -m venv .venv`
2. Activate the virtual environment:
    - Unix/macOS: `source .venv/bin/activate`
    - Windows: `.venv\Scripts\activate`
3. Install the required packages:
   `pip install mkdocs mkdocs-material mkdocs-git-revision-date-localized-plugin mkdocs-redirects`
4. Start the server: `mkdocs serve --config-file docs/mkdocs.yml`

## Docker

1. Build the image using the provided [Dockerfile](Dockerfile):
   `docker build -t chutney/doc docs`
2. Mount the whole repository so that the revision-date plugin can access
   `.git`, and start MkDocs from the documentation directory:

    ``` shell
    docker run --rm -p 8000:8000 \
      -v "${PWD}:/repo" \
      -w /repo/docs \
      chutney/doc
    ```

Visit [http://localhost:8000/](http://localhost:8000/).

# Useful docs

[https://www.mkdocs.org/getting-started/](https://www.mkdocs.org/getting-started/)  
[https://squidfunk.github.io/mkdocs-material/getting-started/](https://squidfunk.github.io/mkdocs-material/getting-started/)
