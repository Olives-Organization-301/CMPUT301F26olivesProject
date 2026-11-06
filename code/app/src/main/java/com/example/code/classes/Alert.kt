package com.example.code.classes

/**
 * Represents abstract notification/alert class
 */
class Alert(
    var alertID: String = "",
    var title: String = "",
    var body: String = "",
    var recipient: Entrant = Entrant(),
    var sender: CampsiteOwner = CampsiteOwner(),
)
