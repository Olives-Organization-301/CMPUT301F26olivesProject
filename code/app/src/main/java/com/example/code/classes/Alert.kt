package com.example.code.classes

// Represents abstract notification/alert class

class Alert (
    public var alertID: String = "",
    var title: String = "",
    var body: String = "",
    var recipient: Entrant = Entrant("", "", "", "", "", "", ""),
    var sender: CampsiteOwner = CampsiteOwner("", "", "")
){
    fun getTitle(): String = title
    fun getBody(): String = body
    fun getRecipient(): Entrant = recipient
    fun getSender(): CampsiteOwner = sender
}