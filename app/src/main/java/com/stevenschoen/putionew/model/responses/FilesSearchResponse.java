package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.ResponseOrError;
import com.stevenschoen.putionew.model.files.PutioFile;

import java.util.List;

public class FilesSearchResponse extends ResponseOrError.BasePutioResponse {
  public List<PutioFile> files;
}
