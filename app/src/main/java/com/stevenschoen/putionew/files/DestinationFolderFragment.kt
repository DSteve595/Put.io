package com.stevenschoen.putionew.files

class DestinationFolderFragment : NewFilesFragment() {
    override val canSelect = false
    override val choosingFolder = true
    override val showSearch = false
    override val showCreateFolder = false
    override val padForFab = false
}