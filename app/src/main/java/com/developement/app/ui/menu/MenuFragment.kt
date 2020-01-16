package com.developement.app.ui.menu

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView

import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.developement.app.Adapter.MyCategoriesAdapter
import com.developement.app.Common.Common
import com.developement.app.Common.SpacesItemDecoration
import com.developement.app.EventBus.MenuItemBack
import com.developement.app.Model.CategoryModel
import com.developement.app.R
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.fragment_category.*
import org.greenrobot.eventbus.EventBus

class MenuFragment : Fragment() {

    private lateinit var menuViewModel: MenuViewModel
    private lateinit var dialog: AlertDialog
    private lateinit var layoutAnimationController: LayoutAnimationController
    private var adapter: MyCategoriesAdapter? = null

    private var recycler_menu: RecyclerView?=null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        menuViewModel =
            ViewModelProviders.of(this).get(MenuViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_category, container, false)

        initViews(root)

        menuViewModel.getMessageError().observe(this, Observer {
           Toast.makeText(context,it, Toast.LENGTH_SHORT).show()
        })
        menuViewModel.getCategoryList().observe(this, Observer {
            dialog.dismiss()
            adapter = MyCategoriesAdapter(context!!, it)
            recycler_menu!!.adapter = adapter
            //recycler_menu!!.layoutAnimation = layoutAnimationController
        })
        return root
    }

    private fun initViews(root:View) {
        setHasOptionsMenu(true)
        dialog = SpotsDialog.Builder().setContext(context)
            .setCancelable(false).build()
        dialog.show()
        layoutAnimationController =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)
        recycler_menu = root.findViewById(R.id.recycler_menu) as RecyclerView
        recycler_menu!!.setHasFixedSize(true)
        val layoutManager = GridLayoutManager(context, 2)
        layoutManager.orientation = RecyclerView.VERTICAL
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter != null) {
                    when (adapter!!.getItemViewType(position)) {
                        Common.DEFAULT_COLUMN_COUNT -> 1
                        Common.FULL_WIDTH_COLUMN -> 2
                        else -> -1
                    }
                } else
                    -1
            }

        }
        recycler_menu!!.layoutManager = layoutManager
        recycler_menu!!.addItemDecoration(SpacesItemDecoration(8))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.search_menu, menu)

        val menuItem = menu.findItem(R.id.action_search)

        val searchManager = activity!!.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menuItem.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity!!.componentName))

        //event
        searchView.setOnQueryTextListener(object:SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(s: String?): Boolean {
                startSearch(s!!)
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }

        })

        // clear texts Stringd when click button
        val closeButton = searchView.findViewById<View>(R.id.search_close_btn) as ImageView
        closeButton.setOnClickListener {
            val ed = searchView.findViewById<View>(R.id.search_src_text) as EditText

            //clear text
            ed.setText("")

            //clear query
            searchView.setQuery("", false)

            // collapse the action view
            searchView.onActionViewCollapsed()

            //collapse the search widget
            menuItem.collapseActionView()

            //restore results to original
            menuViewModel.loadCategory()
        }
    }

    private fun startSearch(s: String) {
        val resultCategory = ArrayList<CategoryModel>()
        for (i in 0 until adapter!!.getCategoryList().size)
        {
            val categoryModel = adapter!!.getCategoryList()[i]
            if (categoryModel.name!!.toLowerCase().contains(s))
                resultCategory.add(categoryModel)
        }
        menuViewModel.getCategoryList().value = resultCategory
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }
}