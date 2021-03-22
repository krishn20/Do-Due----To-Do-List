package com.example.doDue.util

import androidx.appcompat.widget.SearchView

/* We created this function as an extension so that we don't have to write this in the Fragment code and can just call this function there.
*
* We could have used the pre-defined SearchView.OnQueryTextListener() there, but it doesn't provide the ability to work upon it's member functions
* (i.e. onQueryTextChange() in our case). So to do so, we needed to create a self- defined function(SearchView.OnQueryTextChanged()) that can take
* another function ("listener" lambda, which takes a String value and returns Unit i.e. nothing/null.
* From here we can use this String value in the Tasks Fragment as it is available from the 'listener' lambda.) which then can be used inside this member function(onQueryTextChange())
* to perform our operation.
*
* inline keyword is used when we use lambdas in our code, but don't want them to be created as objects and thus be allocated memory and thus increase runtime overhead.
* Instead inline just copies the function code defined (the lambda "listener" here) at the place where the function is used (inside onQueryTextChange() here),
* thus preventing overheads.
*
* crossinline allows us to use the lambda function INSIDE the members of it's direct scope.
*
* listener: (String) -> Unit is a lambda expression. */


inline fun SearchView.onQueryTextChanged(crossinline listener: (String) -> Unit)
{

    this.setOnQueryTextListener(object : SearchView.OnQueryTextListener
    {

        override fun onQueryTextSubmit(query: String?): Boolean {
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            listener(newText.orEmpty())
            return true
        }

    })
}