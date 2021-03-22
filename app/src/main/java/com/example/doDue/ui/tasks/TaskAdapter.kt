package com.example.doDue.ui.tasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.doDue.data.Task
import com.example.doDue.databinding.TaskListItemsBinding

/* We use ListAdapter instead of normal RecyclerView.Adapter as it is more flexible to the new Flow of data being sent and the related animations.
* When the DB is changed, the full list is sent as a Flow and not just one particular changed/added item. This can be handled properly by ListAdapter only. */

/* ListAdapter takes 2 parameters, the type of item/object (Task in this case) and the ViewHolder of the same. DiffCallback is a DiffUtil class also to be instantiated.*/

/* TaskAdapter will also take the "onItemClickListener" interface type object so as to use it's onClick functions created on the tasks in the list. */

class TaskAdapter(private val listener: OnItemClickListener): ListAdapter<Task, TaskAdapter.TaskViewHolder>(
    DiffCallback()
) {

    /* As we need to pass the view of each task_item (while using ViewBinding), we do so normally as always, and then pass it to the
    ViewHolder (which expects a binding type object). This is then returned after the creating and binding of ViewHolder (using VH and BindVH function respectively). */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = TaskListItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    /* REMEMBER - TaskViewHolder is just a class and the onCreateVH and onBindVH are functions which use this VH class. */

    /* This is the ViewHolder class nested in the Adapter class only. Now we normally use the layout of the tasks_list_items and pass it as a view ot he ViewHolder,
    * and then use it's child views using findViewByIds. But here we use ViewBinding to do the same. This makes the code shorter, easier and compile time-safe.*/

    /* The bindings are created automatically (and their names are formatted into proper CamelCase characters). */
    /* The apply function applies the binding properties on it's scope and thus we can use them directly inside the binding.apply brackets/scope. */

    inner class TaskViewHolder(private val binding: TaskListItemsBinding): RecyclerView.ViewHolder(binding.root)
    {

        /* Whenever we create a ViewHolder for a task, we would want these onClickListeners from the interface to always be implemented on every item/task.
        * The interface is made in this TaskAdapter class, the definition for them is provided in the fragment
        * and they are called in the TaskAdapter and then used here.
        * Therefore, we initialize(init) every VH with these onClickListeners (for itemClick and checkBox click).
        *
        * Mostly we define these operations (and that too without any interface) inside the onBindViewHolder(). This works too, but is very inefficient.
        * This is because then these will be called again and again for every item created. Thus causing a lot of bg work.
        * Whereas we create them here inside the TaskViewHolder, because VHs are created and reused between different list items, thus reducing the amount drastically.
        * Also, the functionality still works for every task in the list, as it is a part of the VH only. */

        init {
            binding.apply {

                root.setOnClickListener {
                    val position = adapterPosition
                    if(position != RecyclerView.NO_POSITION){
                        val task = getItem(position)
                        listener.onItemClick(task)
                    }
                }

                checkboxItem.setOnClickListener {
                    val position = adapterPosition
                    if(position != RecyclerView.NO_POSITION){
                        val task = getItem(position)
                        listener.onCheckBoxClick(task, checkboxItem.isChecked)
                    }
                }

            }
        }


        fun bind(task: Task)
        {
            binding.apply{
                checkboxItem.isChecked = task.completed
                itemDescription.paint.isStrikeThruText = task.completed
                itemDescription.text = task.name
                importantTasksImg.isVisible = task.priority
            }
        }

    }

    /* Here, we create this interface with the required functions that would help remember checked items and help open selected items. This interface is
    * then implemented by our Fragment, from where we can use these functions and pass the updated list to the Adapter. */
    /* The reason why we create this interface here and make use of these functions in here(rather than making them in the Fragment) is that, this allows our Fragment
    * and Adapter to be decoupled from each other, and thus we can use this Adapter elsewhere with other Fragments as well, which might use these same functions.  */

    interface OnItemClickListener {
        fun onItemClick(task: Task)
        fun onCheckBoxClick(task: Task, isChecked: Boolean)
    }

    /* As the ListAdapter is smart to make changes to the UI according to the coming Flow, it needs some instruction about what to do in case
    the items or their contents are the same. For this we have to make a DiffUtil class. */
    /* In case the contents are same, we check using == operator and thus it will change the item if not the same.
    * Similarly is the case for the whole Item. */
    /* We used both the "=" (new and easy Kotlin shortcut) and the "return" methods of writing a function here. */

    class DiffCallback: DiffUtil.ItemCallback<Task>(){

        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean =
            oldItem == newItem

    }

}