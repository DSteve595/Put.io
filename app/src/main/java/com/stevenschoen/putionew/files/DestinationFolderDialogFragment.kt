package com.stevenschoen.putionew.files

class DestinationFolderDialogFragment : NewFilesFragment() {
    override val canSelect = false
    override val showSearch = false
    override val showCreateFolder = false
    override val padForFab = false
}