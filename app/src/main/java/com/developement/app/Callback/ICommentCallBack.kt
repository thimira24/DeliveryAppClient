package com.developement.app.Callback

import com.developement.app.Model.CommentModel

interface ICommentCallBack {
    fun onCommentLoadSuccess(commentList: List<CommentModel>)
    fun onCommentLoadFailed(message:String)
}