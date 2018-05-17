; https://github.com/per1234/batch-smart-resize
(define (script-fu-batch-image-hash-form sourcePath destinationPath filenameModifier )
  (define (smart-resize fileCount sourceFiles)
    (let*
      (
        (filename (car sourceFiles))
        (image (car (gimp-file-load RUN-NONINTERACTIVE filename filename)))
      )
      (gimp-image-undo-disable image)

      ;crop to mask if one exists
      (if (not (= (car (gimp-layer-get-mask (car (gimp-image-get-active-layer image)))) -1)) (plug-in-autocrop RUN-NONINTERACTIVE image (car (gimp-layer-get-mask (car (gimp-image-get-active-layer image))))))


      ;image manipulation
      (gimp-image-scale image 256 256)  ;scale image to the output dimensions
      (gimp-image-convert-grayscale image)
      (gimp-image-flatten image)  ;flatten the layers


      (let*
        (
          ;format filename - strip source extension(from http://stackoverflow.com/questions/1386293/how-to-parse-out-base-file-name-using-script-fu), add filename modifier and destination path
          (outputFilenameNoExtension
            (string-append
              (string-append destinationPath "/")
              (unbreakupstr
                (reverse
                  (cdr
                    (reverse
                      (strbreakup
                        (car
                          (reverse
                            (strbreakup filename (if isLinux "/" "\\"))
                          )
                        )
                        "."
                      )
                    )
                  )
                )
                "."
              )
              filenameModifier
            )
          )
        )

        ;save file
        (let* 
          (
            (outputFilename (string-append outputFilenameNoExtension ".pgm"))
          )
          ;add the new extension
          ;file-pgm-save parameters
          ;The run mode(RUN-INTERACTIVE(0), RUN-NONINTERACTIVE(1))
          ;Input image
          ;Drawable to save
          ;filename
          ;raw-filename - this doesn't appear to do anything
          ;raw or ASCII type
          (file-pgm-save RUN-NONINTERACTIVE image (car (gimp-image-get-active-drawable image)) outputFilename outputFilename 1)
        )
      )
      (gimp-image-delete image)
    )
    (if (= fileCount 1) 1 (smart-resize (- fileCount 1) (cdr sourceFiles)))  ;determine whether to continue the loop
  )

  ;detect OS type(from http://www.gimp.org/tutorials/AutomatedJpgToXcf/)
  (define isLinux
    (>
      (length (strbreakup sourcePath "/" ) )  ;returns the number of pieces the string is broken into
      (length (strbreakup sourcePath "\\" ) )
    )
  )
  (define sourceFilesGlob (file-glob (if isLinux (string-append sourcePath "/*.*") (string-append sourcePath "\\*.*")) 0))
  (if (pair? (car (cdr sourceFilesGlob)))  ;check for valid source folder(if this script is called from another script they may have passed an invalid path and it's much more helpful to return a meaningful error message)
    (smart-resize (car sourceFilesGlob) (car (cdr sourceFilesGlob)))
    (error (string-append "Invalid Source Folder " sourcePath))
  )
)

;dialog
(script-fu-register
  "script-fu-batch-image-hash-form"  ;function name
  "batch-image-hash-form"  ;menu label
  "Resize to 256x256, convert to grayscale, save as PGM"  ;description
  "Seppukku, based on smart-resize by per1234"  ;author
  ""  ;copyright notice
  "2017-11-27"  ;date created
  ""  ;image type
  SF-DIRNAME "Source Folder" ""  ;sourcePath
  SF-DIRNAME "Destination Folder" ""  ;destinationPath
  SF-STRING "Output Filename Modifier(appended)" ""  ;filenameModifier
)

(script-fu-menu-register "script-fu-batch-image-hash-form"
                         "<Image>/File/Create")  ;menu location