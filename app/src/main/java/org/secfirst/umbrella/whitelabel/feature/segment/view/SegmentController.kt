package org.secfirst.umbrella.whitelabel.feature.segment.view

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ShareCompat
import android.support.v4.content.FileProvider
import android.support.v7.widget.GridLayoutManager
import android.view.*
import com.bluelinelabs.conductor.RouterTransaction
import com.commonsware.cwac.anddown.AndDown
import kotlinx.android.synthetic.main.segment_view.*
import kotlinx.android.synthetic.main.share_dialog.view.*
import org.apache.commons.io.FilenameUtils.removeExtension
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.secfirst.umbrella.whitelabel.BuildConfig.APPLICATION_ID
import org.secfirst.umbrella.whitelabel.R
import org.secfirst.umbrella.whitelabel.UmbrellaApplication
import org.secfirst.umbrella.whitelabel.data.database.checklist.Checklist
import org.secfirst.umbrella.whitelabel.data.database.checklist.covertToHTML
import org.secfirst.umbrella.whitelabel.data.database.segment.HostSegmentTabControl
import org.secfirst.umbrella.whitelabel.data.database.segment.Markdown
import org.secfirst.umbrella.whitelabel.feature.base.view.BaseController
import org.secfirst.umbrella.whitelabel.feature.segment.DaggerSegmentComponent
import org.secfirst.umbrella.whitelabel.feature.segment.interactor.SegmentBaseInteractor
import org.secfirst.umbrella.whitelabel.feature.segment.presenter.SegmentBasePresenter
import org.secfirst.umbrella.whitelabel.feature.segment.view.adapter.SegmentAdapter
import org.secfirst.umbrella.whitelabel.misc.createDocument
import org.secfirst.umbrella.whitelabel.misc.initGridView
import java.io.File
import javax.inject.Inject


class SegmentController(bundle: Bundle) : BaseController(bundle), SegmentView {


    @Inject
    internal lateinit var presenter: SegmentBasePresenter<SegmentView, SegmentBaseInteractor>
    private val segmentClick: (Int) -> Unit = this::onSegmentClicked
    private val checklistFavoriteClick: (Boolean) -> Unit = this::onChecklistFavoriteClick
    private val segmentFavoriteClick: (Markdown) -> Unit = this::onSegmentFavoriteClick
    private val checklistShareClick: () -> Unit = this::onChecklistShareClick
    private val segmentShareClick: (Markdown) -> Unit = this::onSegmentShareClick
    private val footClick: (Int) -> Unit = this::onFootClicked
    private val markdownIds by lazy { args.getStringArrayList(EXTRA_SEGMENT) }
    private val checklistId by lazy { args.getString(EXTRA_CHECKLIST) }
    private var checklist: Checklist? = null
    private lateinit var shareDialog: AlertDialog
    private lateinit var shareView: View
    private var indexTab = 0
    private lateinit var hostSegmentTabControl: HostSegmentTabControl

    constructor(markdownIds: ArrayList<String>, checklistId: String) : this(Bundle().apply {
        putStringArrayList(EXTRA_SEGMENT, markdownIds)
        putString(EXTRA_CHECKLIST, checklistId)
    })

    override fun onInject() {
        DaggerSegmentComponent.builder()
                .application(UmbrellaApplication.instance)
                .build()
                .inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        setHasOptionsMenu(true)
        shareView = inflater.inflate(R.layout.share_dialog, container, false)
        shareDialog = AlertDialog
                .Builder(activity)
                .setView(shareView)
                .create()
        presenter.onAttach(this)
        presenter.submitMarkdownsAndChecklist(markdownIds, checklistId)
        return inflater.inflate(R.layout.segment_view, container, false)
    }

    override fun showSegments(markdowns: List<Markdown>, checklist: Checklist?) {
        this.checklist = checklist
        initSegmentView(markdowns)
    }

    private fun initSegmentView(markdowns: List<Markdown>) {
        val sortedMarkdowns = markdowns.sortedWith(compareBy { it.index })
        val segmentAdapter = SegmentAdapter(segmentClick, footClick,
                checklistShareClick, segmentShareClick,
                checklistFavoriteClick, segmentFavoriteClick, checklist, sortedMarkdowns.toMutableList())

        segmentRecyclerView?.initGridView(segmentAdapter)
        setFooterList(segmentAdapter)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        return inflater.inflate(R.menu.search_menu, menu)
    }

    private fun setFooterList(segmentAdapter: SegmentAdapter) {
        val manager = if (segmentRecyclerView?.layoutManager != null)
            segmentRecyclerView?.layoutManager as GridLayoutManager else null

        manager?.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (segmentAdapter.isChecklistFoot(position)) manager?.spanCount ?: 0 else 1
            }
        }
    }

    override fun showSegmentDetail(markdown: Markdown) {
        parentController?.router?.pushController(RouterTransaction.with(SegmentDetailController(markdown)))
    }

    private fun onChecklistFavoriteClick(isFavorite: Boolean) {
        checklist?.favorite = isFavorite
        if (checklist != null)
            presenter.submitChecklistFavorite(checklist!!)
    }


    private fun onChecklistShareClick() {
        val checklistHtml = checklist?.covertToHTML()
        val intent = Intent(Intent.ACTION_SENDTO,
                Uri.parse("mailto:?subject=Checklist&body=${Uri.encode(checklistHtml)}"))
        startActivity(intent)

    }

    private fun onSegmentShareClick(markdown: Markdown) {
        val andDown = AndDown()
        val result = andDown.markdownToHtml(markdown.text, AndDown.HOEDOWN_EXT_QUOTE, 0)
        val doc = Jsoup.parse(result)
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml)
        showShareDialog(doc, markdown.title)
    }

    companion object {
        const val EXTRA_SEGMENT = "selected_segment"
        const val EXTRA_CHECKLIST = "selected_checklist"
        const val EXTRA_SEGMENT_TAB_TITLE = "selected_tab_title"
    }

    fun getTitle(): String = "Lesson"

    fun setIndexTab(position: Int) {
        this.indexTab = position
    }


    private fun shareDocument(fileToShare: File) {
        val pm = context.packageManager
        val uri = FileProvider.getUriForFile(context, APPLICATION_ID, fileToShare)
        val shareIntent = ShareCompat.IntentBuilder.from(activity)
                .setType(context.contentResolver.getType(uri))
                .setStream(uri)
                .intent

        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, removeExtension(fileToShare.name))
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (shareIntent.resolveActivity(pm) != null)
            startActivity(Intent.createChooser(shareIntent, R.string.export_lesson.toString()))

    }

    private fun showShareDialog(doc: Document, title: String) {
        var type = ""
        shareView.pdfRadio.text = context.getString(R.string.pdf_name)
        shareView.htmlRadio.text = context.getString(R.string.html_name)

        shareView.shareDocumentButton.setOnClickListener {
            shareDocument(createDocument(doc, title, type, context))
            shareDialog.dismiss()
        }
        shareView.cancelShareButton.setOnClickListener { shareDialog.dismiss() }
        shareView.shareGroup.setOnCheckedChangeListener { _, checkedId ->
            type = if (shareView.pdfRadio.id == checkedId)
                context.getString(R.string.pdf_name)
            else
                context.getString(R.string.html_name)
        }
        shareDialog.show()
    }

    private fun onSegmentFavoriteClick(markdown: Markdown) = presenter.submitMarkdownFavorite(markdown)

    private fun onFootClicked(position: Int) = hostSegmentTabControl.onTabHostManager(position + 1)

    private fun onSegmentClicked(position: Int) = hostSegmentTabControl.onTabHostManager(position + 1)

    fun setSegmentTabControl(hostSegment: HostSegmentTabControl) {
        hostSegmentTabControl = hostSegment
    }
}
